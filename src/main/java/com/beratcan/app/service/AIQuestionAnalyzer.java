package com.beratcan.app.service;

import com.beratcan.app.constants.AppConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.beratcan.app.dto.response.QuestionAnalysisResult;
import com.beratcan.app.entity.Question;
import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.exception.ExternalServiceException;
import com.beratcan.app.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIQuestionAnalyzer {

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(AppConstants.HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConstants.HTTP_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConstants.HTTP_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    /**
     * Soru görselinden 3 ipucu ve tam çözüm üretir
     * 
     * @param imageFile  Görsel dosyası
     * @param questionId Soru ID (ExamType algılamak için)
     */
    public QuestionAnalysisResult analyzeQuestion(File imageFile, Long questionId) {
        long startTime = System.currentTimeMillis();
        long imageSizeKB = imageFile.length() / 1024;

        log.info("AI analysis started: file={}, size={}KB, model={}",
                imageFile.getName(), imageSizeKB, model);

        try {
            // Görseli base64'e çevir
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // ExamType algıla ve prompt hazırla
            ExamType examType = getExamTypeFromQuestion(questionId);
            String prompt = createHintAndSolutionPrompt(examType);
            int promptLength = prompt.length();

            // OpenAI Vision API isteği (HTTP direkt)
            String requestBody = createVisionApiRequest(prompt, base64Image);

            log.info("Calling OpenAI API: model={}, imageSize={}KB, promptLength={} chars",
                    model, imageSizeKB, promptLength);

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            // Response al
            try (Response response = httpClient.newCall(request).execute()) {
                long duration = System.currentTimeMillis() - startTime;

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    log.error("OpenAI API failed: status={}, duration={}ms, model={}, imageSize={}KB, error={}",
                            response.code(), duration, model, imageSizeKB, errorBody);
                    throw new ExternalServiceException(
                            String.format("OpenAI API hatası: HTTP %d - %s", response.code(), errorBody));
                }

                String responseBody = response.body().string();

                // Parse response
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                String content = jsonResponse.get("choices").get(0).get("message").get("content").asText();

                // Extract token usage if available
                String tokenInfo = extractTokenUsage(jsonResponse);

                // Calculate estimated cost
                double estimatedCost = estimateCost(model, imageSizeKB);

                log.info(
                        "AI analysis completed: duration={}ms, responseLength={} chars, model={}, cost=~${}, tokens={}",
                        duration, content.length(), model, String.format("%.3f", estimatedCost), tokenInfo);

                // Performance warning
                if (duration > 30000) {
                    log.warn("Slow AI response detected: duration={}ms, threshold=30000ms, model={}, imageSize={}KB",
                            duration, model, imageSizeKB);
                }

                // Parse et
                return parseAIResponse(content);
            }

        } catch (ExternalServiceException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error(
                    "AI analysis failed (ExternalServiceException): file={}, duration={}ms, model={}, imageSize={}KB, error={}",
                    imageFile.getName(), duration, model, imageSizeKB, e.getMessage(), e);
            throw e; // ExternalServiceException'ı olduğu gibi fırlat
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("AI analysis error: file={}, duration={}ms, model={}, imageSize={}KB",
                    imageFile.getName(), duration, model, imageSizeKB, e);
            throw new ExternalServiceException("Soru analizi başarısız oldu: " + e.getMessage(), e);
        }
    }

    private String createVisionApiRequest(String prompt, String base64Image) {
        // OpenAI Vision API request body
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", model);
        requestMap.put("max_tokens", 1500);
        requestMap.put("temperature", 0.3);

        List<Map<String, Object>> messages = new ArrayList<>();

        // System message
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content",
                "Sen bir TYT matematik öğretmenisin. Soruları analiz edip öğrencilere yardımcı oluyorsun.");
        messages.add(systemMessage);

        // User message with image
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");

        List<Map<String, Object>> content = new ArrayList<>();

        // Text part
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", prompt);
        content.add(textPart);

        // Image part
        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        Map<String, String> imageUrl = new HashMap<>();
        imageUrl.put("url", "data:image/png;base64," + base64Image);
        imagePart.put("image_url", imageUrl);
        content.add(imagePart);

        userMessage.put("content", content);
        messages.add(userMessage);

        requestMap.put("messages", messages);

        try {
            return objectMapper.writeValueAsString(requestMap);
        } catch (Exception e) {
            log.error("Failed to create request body for OpenAI API", e);
            throw new ExternalServiceException("OpenAI API için request body oluşturulamadı", e);
        }
    }

    /**
     * Question'dan ExamType çıkar (denormalized field'dan direkt okur)
     * Performans için: Question entity'sinde examType field'ı var
     */
    private ExamType getExamTypeFromQuestion(Long questionId) {
        if (questionId == null) {
            return ExamType.DIGER; // Default
        }

        try {
            Question question = questionRepository.findById(questionId).orElse(null);
            if (question == null) {
                return ExamType.DIGER;
            }

            // Denormalized field'dan direkt oku (hızlı)
            ExamType examType = question.getExamType();
            return examType != null ? examType : ExamType.DIGER;
        } catch (Exception e) {
            log.warn("ExamType algılanamadı, questionId={}, error={}", questionId, e.getMessage());
            return ExamType.DIGER;
        }
    }

    /**
     * ExamType'a göre hint ve solution prompt'u oluştur
     */
    private String createHintAndSolutionPrompt(ExamType examType) {
        return switch (examType) {
            case YKS -> createYKSHintPrompt();
            case LGS -> createLGShintPrompt();
            case KPSS -> createKPSSHintPrompt();
            default -> createDIGERHintPrompt();
        };
    }

    /**
     * YKS için hint ve solution prompt'u
     */
    private String createYKSHintPrompt() {
        return """
                Bu YKS (Yükseköğretim Kurumları Sınavı) sınavındaki konularla alakalı olan soruyu analiz et ve şu formatta JSON döndür:

                {
                  "hint1": "İlk ipucu - Soru hangi konuyla ilgiliyse ve soruda o konunun hangi kısmı kullanılıyorsa o kısım ile ilgili bilgi ver. olduğunu belirt",
                  "hint2": "İkinci ipucu - Çözüm stratejisinin ilk adımını anlat",
                  "hint3": "Üçüncü ipucu - Neredeyse çözüme götürecek kritik bilgi",
                  "solution": "Tam çözüm - Adım adım detaylı açıklama"
                }

                KURALLAR:
                1. İpuçları kademeli olarak zorlaşmalı (kolay → orta → zor)
                2. Her ipucu öncekinden daha fazla bilgi vermeli
                3. Çözüm matematiksel olarak kesinlikle doğru olmalı
                4. Türkçe kullan
                5. Sadece JSON döndür, ekstra yorum yapma
                6. Çözümde formülleri açıkça yaz
                7. İpuçları özlü ve net olmalı (maksimum 2-3 cümle)
                8. Çözüm detaylı adım adım olmalı

                ÖRNEKLENDİRME:
                İPUÇU 1: "Bu soru ikinci dereceden bir denklem ve kök sayısını belirleme konusuyla ilgilidir. Kök sayısını bulmak için diskriminant kavramı kullanılır.
                İPUÇU 2: "Denklemi önce standart forma getir: ax² + bx + c = 0. Sonra a, b ve c katsayılarını belirleyip diskriminantı Δ = b² - 4ac formülüyle hesaplamayı düşün."
                İPUÇU 3: "Bu denklem için a = 2, b = -5, c = 3 değerlerini Δ = b² - 4ac formülüne yerleştir: Δ = (-5)² - 4·2·3. Δ > 0 çıkarsa denklemin iki farklı gerçek kökü olduğunu unutma."
                ÇÖZÜM: "Adım 1: Denklemi standart forma getir: 2x² - 5x + 3 = 0
                Adım 2: Katsayıları belirle: a=2, b=-5, c=3
                Adım 3: Diskriminantı hesapla: Δ = b² - 4ac = (-5)² - 4(2)(3) = 25 - 24 = 1
                Adım 4: Δ > 0 olduğu için 2 farklı gerçek kök vardır
                Adım 5: Kökleri bul: x = (5 ± √1) / 4 = (5 ± 1) / 4
                x₁ = 6/4 = 3/2, x₂ = 4/4 = 1
                Cevap: x = 3/2 veya x = 1"

                YKS ÖZEL NOTLAR:
                - Üniversite sınavı seviyesinde açıklamalar yap
                - Formülleri akademik düzeyde açıkla
                - Matematiksel terminolojiyi doğru kullan
                - Çözümde her adımı detaylı ve kesin açıkla
                """;
    }

    /**
     * LGS için hint ve solution prompt'u
     */
    private String createLGShintPrompt() {
        return """
                Bu LGS (Liselere Geçiş Sistemi) sınavındaki konularla alakalı olan soruyu analiz et ve şu formatta JSON döndür:

                {
                  "hint1": "İlk ipucu - Soru hangi konuyla ilgiliyse ve soruda o konunun hangi kısmı kullanılıyorsa o kısım ile ilgili bilgi ver. olduğunu belirt",
                  "hint2": "İkinci ipucu - Çözüm stratejisinin ilk adımını anlat",
                  "hint3": "Üçüncü ipucu - Neredeyse çözüme götürecek kritik bilgi",
                  "solution": "Tam çözüm - Adım adım detaylı açıklama"
                }

                KURALLAR:
                1. Her zaman 3 ipucu üret
                2. İpuçları kademeli olarak zorlaşmalı (kolay → orta → zor)
                3. Her ipucu öncekinden daha fazla bilgi vermeli
                4. Çözüm matematiksel olarak kesinlikle doğru olmalı
                5. Türkçe kullan
                6. Sadece JSON döndür, ekstra yorum yapma
                7. Çözümde formülleri açıkça yaz ve açıkla
                8. İpuçları özlü ve net olmalı (maksimum 2-3 cümle)
                9. Çözüm adım adım detaylı ve açıklama odaklı olmalı

                LGS ÖZEL NOTLAR:
                - Ortaokul seviyesinde açıklamalar yap
                - Daha basit terimler kullan
                - Görsel açıklamalar öner (mümkünse)
                - Her adımı çok net ve anlaşılır açıkla
                - Öğrencinin seviyesine uygun örnekler ver

                ÖRNEKLENDİRME:
                İPUÇU 1: "Bu soruda bir denklem var"
                İPUÇU 2: "Denklemi çözmek için önce bilinmeyeni yalnız bırakmalısın"
                İPUÇU 3: "x'i yalnız bırakmak için her iki taraftan 5 çıkar: x = 10 - 5 = 5"
                ÇÖZÜM: "Adım 1: Denklemi incele: x + 5 = 10
                Adım 2: x'i yalnız bırakmak için eşitliğin her iki tarafından 5 çıkaralım
                Adım 3: x + 5 - 5 = 10 - 5
                Adım 4: x = 5
                Cevap: x = 5"
                """;
    }

    /**
     * KPSS için hint ve solution prompt'u
     */
    private String createKPSSHintPrompt() {
        return """
                Bu KPSS (Kamu Personeli Seçme Sınavı) sınavındaki konularla alakalı olan soruyu analiz et ve şu formatta JSON döndür:

                {
                  "hint1": "İlk ipucu - Soru hangi konuyla ilgiliyse ve soruda o konunun hangi kısmı kullanılıyorsa o kısım ile ilgili bilgi ver. olduğunu belirt",
                  "hint2": "İkinci ipucu - Çözüm stratejisinin ilk adımını anlat",
                  "hint3": "Üçüncü ipucu - Neredeyse çözüme götürecek kritik bilgi",
                  "solution": "Tam çözüm - Adım adım detaylı açıklama"
                }

                KURALLAR:
                1. Her zaman 3 ipucu üret
                2. İpuçları kademeli olarak zorlaşmalı (kolay → orta → zor)
                3. Her ipucu öncekinden daha fazla bilgi vermeli
                4. Çözüm matematiksel olarak kesinlikle doğru olmalı
                5. Türkçe kullan
                6. Sadece JSON döndür, ekstra yorum yapma
                7. Çözümde formülleri açıkça yaz ve açıkla
                8. İpuçları özlü ve net olmalı (maksimum 2-3 cümle)
                9. Çözüm adım adım detaylı ve açıklama odaklı olmalı

                KPSS ÖZEL NOTLAR:
                - Genel kültür ve genel yetenek odaklı açıklamalar
                - Pratik çözüm yolları öner
                - Hızlı çözüm teknikleri belirt (zaman yönetimi için)
                - Her adımı net ve anlaşılır açıkla

                ÖRNEKLENDİRME:
                İPUÇU 1: "Bu tür sorularda önce verilen bilgileri düzenle"
                İPUÇU 2: "Oran-orantı kullanarak çözebilirsin"
                İPUÇU 3: "3/5 = x/10 orantısını kur: x = (3 × 10) / 5 = 6"
                ÇÖZÜM: "Adım 1: Verilen oran: 3/5
                Adım 2: Bilinmeyen değer için orantı kur: 3/5 = x/10
                Adım 3: Çapraz çarpım yap: 3 × 10 = 5 × x
                Adım 4: 30 = 5x
                Adım 5: x = 30 / 5 = 6
                Cevap: x = 6"
                """;
    }

    /**
     * Diğer sınav türleri için genel hint ve solution prompt'u
     */
    private String createDIGERHintPrompt() {
        return """
                Bu soruyu analiz et ve şu formatta JSON döndür:

                {
                  "hint1": "İlk ipucu - Soru hangi konuyla ilgiliyse ve soruda o konunun hangi kısmı kullanılıyorsa o kısım ile ilgili bilgi ver. olduğunu belirt",
                  "hint2": "İkinci ipucu - Çözüm stratejisinin ilk adımını anlat",
                  "hint3": "Üçüncü ipucu - Neredeyse çözüme götürecek kritik bilgi",
                  "solution": "Tam çözüm - Adım adım detaylı açıklama"
                }

                KURALLAR:
                1. Her zaman 3 ipucu üret
                2. İpuçları kademeli olarak zorlaşmalı (kolay → orta → zor)
                3. Her ipucu öncekinden daha fazla bilgi vermeli
                4. Çözüm matematiksel olarak kesinlikle doğru olmalı
                5. Türkçe kullan
                6. Sadece JSON döndür, ekstra yorum yapma
                7. Çözümde formülleri açıkça yaz ve açıkla
                8. İpuçları özlü ve net olmalı (maksimum 2-3 cümle)
                9. Çözüm adım adım detaylı ve açıklama odaklı olmalı

                ÖRNEKLENDİRME:
                İPUÇU 1: "Bu bir ikinci dereceden denklem problemidir"
                İPUÇU 2: "Diskriminant (Δ = b² - 4ac) formülünü kullanarak kök sayısını bulabilirsin"
                İPUÇU 3: "a=2, b=-5, c=3 değerlerini diskriminant formülüne yerleştir: Δ = (-5)² - 4(2)(3)"
                ÇÖZÜM: "Adım 1: Denklemi standart forma getir: 2x² - 5x + 3 = 0
                Adım 2: Katsayıları belirle: a=2, b=-5, c=3
                Adım 3: Diskriminantı hesapla: Δ = b² - 4ac = (-5)² - 4(2)(3) = 25 - 24 = 1
                Adım 4: Δ > 0 olduğu için 2 farklı gerçek kök vardır
                Adım 5: Kökleri bul: x = (5 ± √1) / 4 = (5 ± 1) / 4
                x₁ = 6/4 = 3/2, x₂ = 4/4 = 1
                Cevap: x = 3/2 veya x = 1"
                """;
    }

    private QuestionAnalysisResult parseAIResponse(String aiResponse) {
        try {
            // JSON parse et
            // Önce { ile başlayan ve } ile biten kısmı bul (ekstra text olabilir)
            int startIndex = aiResponse.indexOf('{');
            int endIndex = aiResponse.lastIndexOf('}');

            if (startIndex == -1 || endIndex == -1) {
                throw new ExternalServiceException("OpenAI yanıtında JSON bulunamadı: "
                        + aiResponse.substring(0, Math.min(200, aiResponse.length())));
            }

            String jsonString = aiResponse.substring(startIndex, endIndex + 1);

            QuestionAnalysisResult result = objectMapper.readValue(jsonString, QuestionAnalysisResult.class);

            // Validasyon
            if (result.getHint1() == null || result.getHint1().isEmpty()) {
                throw new ExternalServiceException("OpenAI yanıtında ipucu 1 bulunamadı");
            }
            if (result.getHint2() == null || result.getHint2().isEmpty()) {
                throw new ExternalServiceException("OpenAI yanıtında ipucu 2 bulunamadı");
            }
            if (result.getHint3() == null || result.getHint3().isEmpty()) {
                throw new ExternalServiceException("OpenAI yanıtında ipucu 3 bulunamadı");
            }
            if (result.getSolution() == null || result.getSolution().isEmpty()) {
                throw new ExternalServiceException("OpenAI yanıtında çözüm bulunamadı");
            }

            log.info("AI response parsed successfully: hints=3, solutionLength={} chars",
                    result.getSolution().length());

            return result;

        } catch (ExternalServiceException e) {
            throw e; // ExternalServiceException'ı olduğu gibi fırlat
        } catch (Exception e) {
            log.error("JSON parse error: responseLength={}, error={}", aiResponse.length(), e.getMessage(), e);
            throw new ExternalServiceException("OpenAI yanıtı parse edilemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Model ve görsel boyutuna göre tahmini maliyet hesaplar
     * 
     * @param modelName   Model adı (gpt-4o-mini, gpt-4o, gpt-4)
     * @param imageSizeKB Görsel boyutu (KB)
     * @return Tahmini maliyet (USD)
     */
    private double estimateCost(String modelName, long imageSizeKB) {
        // OpenAI Vision API pricing (approximate):
        // gpt-4o-mini: ~$0.01 per image
        // gpt-4o: ~$0.04 per image
        // gpt-4-vision-preview: ~$0.05 per image

        double baseCost;
        if (modelName.contains("mini")) {
            baseCost = 0.01;
        } else if (modelName.contains("gpt-4o")) {
            baseCost = 0.04;
        } else if (modelName.contains("gpt-4")) {
            baseCost = 0.05;
        } else {
            baseCost = 0.02; // Default estimate
        }

        // Large images may cost slightly more (size multiplier)
        double sizeMultiplier = 1.0;
        if (imageSizeKB > 1024) { // > 1MB
            sizeMultiplier = 1.2;
        } else if (imageSizeKB > 512) { // > 512KB
            sizeMultiplier = 1.1;
        }

        return baseCost * sizeMultiplier;
    }

    /**
     * OpenAI response'dan token kullanım bilgisini çıkartır
     * 
     * @param jsonResponse OpenAI API response
     * @return Token kullanım bilgisi (prompt/completion/total)
     */
    private String extractTokenUsage(JsonNode jsonResponse) {
        try {
            JsonNode usage = jsonResponse.get("usage");
            if (usage != null) {
                int promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : 0;
                int completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : 0;
                int totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").asInt() : 0;

                return String.format("prompt=%d, completion=%d, total=%d",
                        promptTokens, completionTokens, totalTokens);
            }
        } catch (Exception e) {
            log.debug("Could not extract token usage from response", e);
        }

        return "N/A";
    }
}
