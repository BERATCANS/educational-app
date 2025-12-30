package com.beratcan.app.service;

import com.beratcan.app.constants.AppConstants;
import com.beratcan.app.dto.response.AIMessageResponse;
import com.beratcan.app.dto.response.QuestionAIConversationResponse;
import com.beratcan.app.entity.*;
import com.beratcan.app.entity.enums.MessageRole;
import com.beratcan.app.entity.enums.MessageType;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ExternalServiceException;
import com.beratcan.app.exception.QuestionNotFoundException;
import com.beratcan.app.exception.UserNotFoundException;
import com.beratcan.app.repository.QuestionAIConversationRepository;
import com.beratcan.app.repository.QuestionAIMessageRepository;
import com.beratcan.app.repository.QuestionRepository;
import com.beratcan.app.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionAIChatService {

    private final QuestionAIConversationRepository conversationRepository;
    private final QuestionAIMessageRepository messageRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final SpeechProcessingService speechProcessingService;
    private final DailyLimitService dailyLimitService;
    private final FileService fileService;
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

    @Value("${ai.chat.prompt.temperature:0.5}")
    private double chatTemperature;

    @Value("${ai.chat.response.max.words:200}")
    private int responseMaxWords;

    @Value("${ai.chat.response.medium.words:400}")
    private int responseMediumWords;

    @Value("${file.upload.dir:uploads/}")
    private String uploadDir;

    /**
     * Conversation getir veya oluştur
     */
    @Transactional
    public QuestionAIConversationResponse getOrCreateConversation(Long questionId, Long userId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        QuestionAIConversation conversation = conversationRepository
                .findByQuestionIdAndUserId(questionId, userId)
                .orElseGet(() -> {
                    QuestionAIConversation newConv = new QuestionAIConversation();
                    newConv.setQuestion(question);
                    newConv.setUser(user);
                    return conversationRepository.save(newConv);
                });

        return toConversationResponse(conversation);
    }

    /**
     * Mesaj gönder ve AI yanıtı al
     */
    @Transactional
    public AIMessageResponse sendMessage(Long questionId, Long userId, String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new BadRequestException("Mesaj boş olamaz");
        }

        // Günlük AI konuşma limitini kontrol et
        dailyLimitService.checkDailyAiConversationLimit(userId);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Conversation getir veya oluştur
        QuestionAIConversation conversation = conversationRepository
                .findByQuestionIdAndUserId(questionId, userId)
                .orElseGet(() -> {
                    QuestionAIConversation newConv = new QuestionAIConversation();
                    newConv.setQuestion(question);
                    newConv.setUser(user);
                    return conversationRepository.save(newConv);
                });

        // Kullanıcı mesajını kaydet
        QuestionAIMessage userMsg = new QuestionAIMessage();
        userMsg.setConversation(conversation);
        userMsg.setContent(userMessage.trim());
        userMsg.setRole(MessageRole.USER);
        userMsg.setMessageType(MessageType.TEXT); // Text mesaj
        messageRepository.save(userMsg);

        // Conversation history al
        List<QuestionAIMessage> history = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        // Soru context'i oluştur (history ile birlikte)
        String context = buildStudentChatContext(question, history);

        // Soru görselini base64'e çevir
        String base64Image = null;
        String imageMimeType = null; // MIME type'ı da sakla
        try {
            if (question.getImageUrl() != null && !question.getImageUrl().trim().isEmpty()) {
                // StorageService üzerinden dosyayı oku
                try {
                    org.springframework.core.io.Resource imageResource = fileService.getFile(question.getImageUrl());

                    if (imageResource.exists() && imageResource.isReadable()) {
                        byte[] imageBytes = imageResource.getInputStream().readAllBytes();
                        base64Image = Base64.getEncoder().encodeToString(imageBytes);

                        // Dosya uzantısına göre MIME type belirle
                        String fileName = question.getImageUrl().toLowerCase();
                        if (fileName.endsWith(".png")) {
                            imageMimeType = "image/png";
                        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                            imageMimeType = "image/jpeg";
                        } else if (fileName.endsWith(".gif")) {
                            imageMimeType = "image/gif";
                        } else if (fileName.endsWith(".webp")) {
                            imageMimeType = "image/webp";
                        } else {
                            // Varsayılan olarak JPEG (çoğu görsel JPEG)
                            imageMimeType = "image/jpeg";
                            log.warn("Bilinmeyen görsel formatı, JPEG olarak varsayılıyor: questionId={}, fileName={}",
                                    questionId, fileName);
                        }

                        log.debug("Soru görseli base64'e çevrildi: questionId={}, size={}KB, mimeType={}",
                                questionId, imageBytes.length / 1024, imageMimeType);
                    } else {
                        log.warn("Soru görseli bulunamadı veya okunamıyor: questionId={}, imageUrl={}",
                                questionId, question.getImageUrl());
                    }
                } catch (Exception ex) {
                    log.warn("Soru görseli okunamadı: questionId={}, imageUrl={}, error={}",
                            questionId, question.getImageUrl(), ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Soru görseli okunamadı: questionId={}, error={}", questionId, e.getMessage());
            // Görsel okunamazsa devam et, sadece text ile gönder
        }

        // OpenAI API çağrısı
        String aiResponse;
        Integer tokenCount;
        try {
            Map<String, Object> result = callOpenAIChatAPI(context, history, question, base64Image, imageMimeType);
            aiResponse = (String) result.get("content");
            tokenCount = (Integer) result.get("tokenCount");
        } catch (Exception e) {
            log.error("OpenAI API çağrısı başarısız: {}", e.getMessage(), e);
            throw new ExternalServiceException("AI yanıtı alınamadı: " + e.getMessage());
        }

        // AI yanıtını kaydet
        QuestionAIMessage aiMsg = new QuestionAIMessage();
        aiMsg.setConversation(conversation);
        aiMsg.setContent(aiResponse);
        aiMsg.setRole(MessageRole.ASSISTANT);
        aiMsg.setMessageType(MessageType.ASSISTANT_TEXT); // Text yanıt
        aiMsg.setTokenCount(tokenCount);
        messageRepository.save(aiMsg);

        // Conversation'ı güncelle
        conversation.setLastMessageAt(aiMsg.getCreatedAt());
        conversationRepository.save(conversation);

        return toMessageResponse(aiMsg);
    }

    /**
     * Conversation geçmişini getir
     */
    @Transactional(readOnly = true)
    public List<AIMessageResponse> getMessages(Long questionId, Long userId) {
        QuestionAIConversation conversation = conversationRepository
                .findByQuestionIdAndUserId(questionId, userId)
                .orElseThrow(() -> new BadRequestException("Conversation bulunamadı"));

        List<QuestionAIMessage> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        return messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Mesaj gönder ve AI yanıtını stream et (SSE)
     */
    @Async("aiAnalysisExecutor")
    @Transactional
    public void sendMessageStream(Long questionId, Long userId, String userMessage, SseEmitter emitter) {
        try {
            // Validation
            dailyLimitService.checkDailyAiConversationLimit(userId);

            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new QuestionNotFoundException(questionId));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));

            // Conversation getir/oluştur
            QuestionAIConversation conversation = conversationRepository
                    .findByQuestionIdAndUserId(questionId, userId)
                    .orElseGet(() -> {
                        QuestionAIConversation newConv = new QuestionAIConversation();
                        newConv.setQuestion(question);
                        newConv.setUser(user);
                        return conversationRepository.save(newConv);
                    });

            // Kullanıcı mesajını kaydet
            QuestionAIMessage userMsg = new QuestionAIMessage();
            userMsg.setConversation(conversation);
            userMsg.setContent(userMessage.trim());
            userMsg.setRole(MessageRole.USER);
            userMsg.setMessageType(MessageType.TEXT);
            messageRepository.save(userMsg);

            // History ve context
            List<QuestionAIMessage> history = messageRepository
                    .findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            String context = buildStudentChatContext(question, history);

            // Görsel hazırla
            String base64Image = prepareQuestionImage(question);
            String imageMimeType = detectImageMimeType(question.getImageUrl());

            // OpenAI Streaming API çağrısı
            callOpenAIChatAPIStream(context, history, question, base64Image, imageMimeType,
                    emitter, conversation);

        } catch (Exception e) {
            log.error("Stream error: questionId={}, userId={}, error={}",
                    questionId, userId, e.getMessage(), e);
            try {
                emitter.completeWithError(e);
            } catch (Exception ex) {
                log.error("Emitter error notification failed", ex);
            }
        }
    }

    /**
     * OpenAI Chat API - Streaming Mode
     */
    private void callOpenAIChatAPIStream(String context, List<QuestionAIMessage> history,
            Question question, String base64Image, String imageMimeType,
            SseEmitter emitter, QuestionAIConversation conversation) {
        long startTime = System.currentTimeMillis();

        try {
            // Messages listesi oluştur
            List<Map<String, Object>> messages = buildMessagesForAPI(context, history,
                    base64Image, imageMimeType);

            // Request body - STREAM=TRUE ekle
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("model", model);
            requestMap.put("messages", messages);
            requestMap.put("max_tokens", 1000);
            requestMap.put("temperature", chatTemperature);
            requestMap.put("stream", true); // STREAMING ENABLE

            String requestBody = objectMapper.writeValueAsString(requestMap);

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            StringBuilder fullResponse = new StringBuilder();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    log.error("OpenAI streaming API failed: status={}, error={}", response.code(), errorBody);
                    emitter.completeWithError(new ExternalServiceException(
                            "OpenAI API error: " + response.code()));
                    return;
                }

                // Stream'i satır satır oku
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body().byteStream()));
                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);

                        if ("[DONE]".equals(data)) {
                            break;
                        }

                        // JSON parse
                        JsonNode jsonChunk = objectMapper.readTree(data);
                        JsonNode choices = jsonChunk.get("choices");
                        if (choices != null && choices.size() > 0) {
                            JsonNode delta = choices.get(0).get("delta");

                            if (delta != null && delta.has("content")) {
                                String chunk = delta.get("content").asText();
                                fullResponse.append(chunk);

                                // Frontend'e chunk gönder
                                Map<String, String> event = new HashMap<>();
                                event.put("type", "chunk");
                                event.put("content", chunk);

                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(event));
                            }
                        }
                    }
                }

                // Tam yanıtı DB'ye kaydet
                QuestionAIMessage aiMsg = new QuestionAIMessage();
                aiMsg.setConversation(conversation);
                aiMsg.setContent(fullResponse.toString());
                aiMsg.setRole(MessageRole.ASSISTANT);
                aiMsg.setMessageType(MessageType.ASSISTANT_TEXT);
                messageRepository.save(aiMsg);

                // Conversation güncelle
                conversation.setLastMessageAt(aiMsg.getCreatedAt());
                conversationRepository.save(conversation);

                // Duration hesapla
                long duration = System.currentTimeMillis() - startTime;
                log.info("OpenAI streaming completed: duration={}ms, responseLength={}",
                        duration, fullResponse.length());

                // Stream'i tamamla
                Map<String, Object> doneEvent = new HashMap<>();
                doneEvent.put("type", "done");
                doneEvent.put("messageId", aiMsg.getId());
                emitter.send(SseEmitter.event().name("done").data(doneEvent));
                emitter.complete();

            }
        } catch (Exception e) {
            log.error("Streaming error: {}", e.getMessage(), e);
            try {
                emitter.completeWithError(e);
            } catch (Exception ex) {
                log.error("Emitter error notification failed", ex);
            }
        }
    }

    /**
     * Öğrenci chat için detaylı context oluştur
     */
    private String buildStudentChatContext(Question question, List<QuestionAIMessage> history) {
        StringBuilder context = new StringBuilder();

        // Soru bilgileri
        context.append("=== SORU BİLGİLERİ ===\n");
        context.append("Soru ID: ").append(question.getId()).append("\n");
        context.append("Doğru Cevap: ").append(question.getCorrectAnswer()).append("\n\n");

        // Mevcut ipuçları
        if (question.getHints() != null && !question.getHints().isEmpty()) {
            context.append("Mevcut İpuçları:\n");
            question.getHints().stream()
                    .sorted((h1, h2) -> Integer.compare(h1.getSequenceNumber(), h2.getSequenceNumber()))
                    .forEach(hint -> context.append("- İpucu ").append(hint.getSequenceNumber())
                            .append(": ").append(hint.getText()).append("\n"));
            context.append("\n");
        }

        // Açıklama
        if (question.getExplanation() != null && question.getExplanation().getText() != null) {
            context.append("Çözüm Açıklaması: ").append(question.getExplanation().getText()).append("\n\n");
        }

        // Rol ve görev tanımı
        context.append("=== ROL VE GÖREV ===\n");
        context.append("Sen bir matematik öğretmenisin. Öğrenciye bu soru hakkında yardımcı oluyorsun.\n");
        context.append("Amacın öğrencinin soruyu anlamasını ve çözmesini sağlamak.\n");
        context.append("Öğrencinin her sorusuna doğrudan, net ve anlaşılır şekilde yanıt ver.\n\n");

        // Öğretim prensipleri
        context.append("=== ÖĞRETİM PRENSİPLERİ ===\n");
        context.append("1. Sokratik yöntem kullan: Öğrenciyi düşündür, gerektiğinde rehberlik et\n");
        context.append("2. Esnek yaklaşım: Öğrencinin sorusuna göre gerekirse detaylı, gerekirse kısa açıkla\n");
        context.append("3. Örneklerle açıkla: Soyut kavramları somut örneklerle göster\n");
        context.append("4. Pozitif ve destekleyici ol: Öğrenciyi cesaretlendir\n");
        context.append("5. Hataları öğrenme fırsatı olarak gör: Yapıcı geri bildirim ver\n\n");

        // Yanıt kuralları
        context.append("=== YANIT KURALLARI ===\n");
        context.append("1. Her zaman Türkçe yanıt ver\n");
        context.append("2. Öğrencinin sorusuna doğrudan odaklan: Ne sorduysa ona yanıt ver\n");
        context.append("3. Gerekli detay seviyesini sen belirle: Öğrencinin ihtiyacına göre uzun veya kısa olabilir\n");
        context.append("4. YANITLARINI MARKDOWN FORMATINDA YAZ:\n");
        context.append("   - Başlıklar için #, ##, ### kullan\n");
        context.append("   - Liste için - veya * kullan\n");
        context.append("   - Kod blokları için ``` kullan\n");
        context.append("   - Kalın yazı için **text** kullan\n");
        context.append("   - İtalik için *text* kullan\n");
        context.append("5. Matematik formüllerini ve denklemleri TUTARLI olarak LaTeX formatında yaz:\n");
        context.append("   - Inline formüller için $...$ kullan (örnek: $x = 5$)\n");
        context.append(
                "   - Block formüller için $$...$$ kullan (örnek: $$\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}$$)\n");
        context.append(
                "   - Örnekler: $x = \\frac{-b \\pm \\sqrt{b^2-4ac}}{2a}$, $E = mc^2$, $\\sum_{i=1}^{n} i = \\frac{n(n+1)}{2}$\n");
        context.append("   - Formülleri açıkla: Sadece yazma, ne anlama geldiğini de söyle\n");
        context.append("6. Öğrencinin seviyesine uygun dil kullan\n");
        context.append("7. Emoji kullanma, profesyonel kal\n");
        context.append("8. Maksimum ").append(responseMaxWords).append(" kelime içinde kal\n");
        context.append("9. ÖNEMLİ: Tüm matematiksel ifadeleri LaTeX formatında yaz, düz metin olarak yazma\n\n");
        return context.toString();
    }

    /**
     * Son N mesajı al (hem USER hem ASSISTANT)
     */
    private List<QuestionAIMessage> pickRecentHistory(List<QuestionAIMessage> history, int maxMessages) {
        if (history.size() <= maxMessages) {
            return history;
        }
        return history.subList(history.size() - maxMessages, history.size());
    }

    /**
     * OpenAI Chat Completions API çağrısı
     * 
     * Değişiklikler:
     * - History'den son 5 mesaj alınıyor (USER + ASSISTANT)
     * - Görsel sadece ilk mesajda gönderiliyor (history boşsa veya çok kısaysa)
     * - Tam çözüm kontrolü kaldırıldı, AI kendisi karar verecek
     */
    private Map<String, Object> callOpenAIChatAPI(String context, List<QuestionAIMessage> history,
            Question question, String base64Image, String imageMimeType) {
        long startTime = System.currentTimeMillis();

        try {
            // Messages listesi oluştur
            List<Map<String, Object>> messages = new ArrayList<>();

            // System message (context)
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", context);
            messages.add(systemMessage);

            // Conversation history: Son 5 mesaj (hem USER hem ASSISTANT)
            List<QuestionAIMessage> recentHistory = pickRecentHistory(history, 5);

            // Görsel gönderim kontrolü:
            // - History boşsa (ilk mesaj) -> görsel gönder
            // - History'de sadece 1 mesaj varsa (ilk USER mesajı, henüz cevap yok) ->
            // görsel gönder
            // - Diğer durumlarda görsel gönderme (zaten ilk mesajda gönderilmiş)
            boolean shouldSendImage = base64Image != null &&
                    (recentHistory.isEmpty() ||
                            (recentHistory.size() == 1 && recentHistory.get(0).getRole() == MessageRole.USER));

            // History mesajlarını ekle
            boolean imageSent = false;
            for (QuestionAIMessage msg : recentHistory) {
                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("role", msg.getRole().name().toLowerCase());

                // İlk USER mesajıysa ve görsel gönderilmesi gerekiyorsa, görseli ekle
                if (shouldSendImage && !imageSent && msg.getRole() == MessageRole.USER) {
                    List<Map<String, Object>> content = new ArrayList<>();

                    // Text part
                    Map<String, Object> textPart = new HashMap<>();
                    textPart.put("type", "text");
                    textPart.put("text", msg.getContent());
                    content.add(textPart);

                    // Image part
                    Map<String, Object> imagePart = new HashMap<>();
                    imagePart.put("type", "image_url");
                    Map<String, String> imageUrl = new HashMap<>();

                    String mime = (imageMimeType != null && !imageMimeType.isEmpty())
                            ? imageMimeType
                            : "image/jpeg";
                    imageUrl.put("url", "data:" + mime + ";base64," + base64Image);
                    imagePart.put("image_url", imageUrl);
                    content.add(imagePart);

                    msgMap.put("content", content);
                    imageSent = true;
                } else {
                    // Normal text mesaj
                    msgMap.put("content", msg.getContent());
                }

                messages.add(msgMap);
            }

            // Request body
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("model", model);
            requestMap.put("messages", messages);
            requestMap.put("max_tokens", 1000); // AI kendi karar verecek, sabit limit
            requestMap.put("temperature", chatTemperature);

            String requestBody = objectMapper.writeValueAsString(requestMap);

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
                    log.error("OpenAI API failed: status={}, duration={}ms, error={}",
                            response.code(), duration, errorBody);
                    throw new ExternalServiceException(
                            String.format("OpenAI API hatası: HTTP %d - %s", response.code(), errorBody));
                }

                String responseBody = response.body().string();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);

                // Content çıkar
                String content = jsonResponse.get("choices").get(0).get("message").get("content").asText();

                // Token kullanımı çıkar
                Integer tokenCount = null;
                Integer promptTokens = null;
                Integer completionTokens = null;
                if (jsonResponse.has("usage")) {
                    JsonNode usage = jsonResponse.get("usage");
                    if (usage.has("total_tokens")) {
                        tokenCount = usage.get("total_tokens").asInt();
                    }
                    if (usage.has("prompt_tokens")) {
                        promptTokens = usage.get("prompt_tokens").asInt();
                    }
                    if (usage.has("completion_tokens")) {
                        completionTokens = usage.get("completion_tokens").asInt();
                    }
                }

                // Estimated cost calculation
                double estimatedCost = estimateAICost(model, promptTokens != null ? promptTokens : 0,
                        completionTokens != null ? completionTokens : 0);

                log.info(
                        "OpenAI API call: model={} | duration={}ms | tokens={} (prompt={}, completion={}) | estimatedCost=${}",
                        model, duration, tokenCount != null ? tokenCount : 0,
                        promptTokens != null ? promptTokens : 0,
                        completionTokens != null ? completionTokens : 0,
                        String.format("%.4f", estimatedCost));

                Map<String, Object> result = new HashMap<>();
                result.put("content", content);
                result.put("tokenCount", tokenCount);
                return result;
            }

        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI API çağrısı sırasında hata: {}", e.getMessage(), e);
            throw new ExternalServiceException("OpenAI API çağrısı başarısız: " + e.getMessage(), e);
        }
    }

    /**
     * Entity'yi Response DTO'ya çevir
     */
    private QuestionAIConversationResponse toConversationResponse(QuestionAIConversation conversation) {
        QuestionAIConversationResponse response = new QuestionAIConversationResponse();
        response.setConversationId(conversation.getId());
        response.setQuestionId(conversation.getQuestion().getId());
        response.setCreatedAt(conversation.getCreatedAt());
        response.setLastMessageAt(conversation.getLastMessageAt());

        List<AIMessageResponse> messages = conversation.getMessages().stream()
                .sorted(Comparator.comparing(QuestionAIMessage::getCreatedAt))
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
        response.setMessages(messages);

        return response;
    }

    /**
     * Voice mesaj gönder ve AI yanıtı al (audio olarak)
     */
    @Transactional
    public AIMessageResponse sendVoiceMessage(Long questionId, Long userId, MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BadRequestException("Audio dosyası boş olamaz");
        }

        // Günlük AI konuşma limitini kontrol et
        dailyLimitService.checkDailyAiConversationLimit(userId);

        log.info("Voice mesaj alındı: questionId={}, userId={}, filename={}, size={}KB",
                questionId, userId, audioFile.getOriginalFilename(), audioFile.getSize() / 1024);

        // 1. Audio'yu text'e çevir (Whisper API)
        String userMessageText;
        try {
            userMessageText = speechProcessingService.speechToText(audioFile);
            log.info("Voice mesaj text'e çevrildi: questionId={}, textLength={} chars",
                    questionId, userMessageText.length());
        } catch (Exception e) {
            log.error("Voice mesaj text'e çevrilemedi: questionId={}, error={}", questionId, e.getMessage(), e);
            throw new BadRequestException("Voice mesaj text'e çevrilemedi: " + e.getMessage());
        }

        if (userMessageText == null || userMessageText.trim().isEmpty()) {
            throw new BadRequestException("Voice mesajdan text çıkarılamadı");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Conversation getir veya oluştur
        QuestionAIConversation conversation = conversationRepository
                .findByQuestionIdAndUserId(questionId, userId)
                .orElseGet(() -> {
                    QuestionAIConversation newConv = new QuestionAIConversation();
                    newConv.setQuestion(question);
                    newConv.setUser(user);
                    return conversationRepository.save(newConv);
                });

        // 2. Kullanıcı voice mesajını text olarak kaydet
        QuestionAIMessage userMsg = new QuestionAIMessage();
        userMsg.setConversation(conversation);
        userMsg.setContent(userMessageText.trim());
        userMsg.setRole(MessageRole.USER);
        userMsg.setMessageType(MessageType.VOICE_INPUT); // Voice mesajdan çevrilmiş text
        messageRepository.save(userMsg);

        // 3. Conversation history al
        List<QuestionAIMessage> history = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        // 4. Soru context'i oluştur
        String context = buildStudentChatContext(question, history);

        // 5. Soru görselini base64'e çevir (mevcut logic)
        String base64Image = null;
        String imageMimeType = null;
        try {
            if (question.getImageUrl() != null && !question.getImageUrl().trim().isEmpty()) {
                // StorageService üzerinden dosyayı oku
                try {
                    org.springframework.core.io.Resource imageResource = fileService.getFile(question.getImageUrl());

                    if (imageResource.exists() && imageResource.isReadable()) {
                        byte[] imageBytes = imageResource.getInputStream().readAllBytes();
                        base64Image = Base64.getEncoder().encodeToString(imageBytes);

                        String fileName = question.getImageUrl().toLowerCase();
                        if (fileName.endsWith(".png")) {
                            imageMimeType = "image/png";
                        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                            imageMimeType = "image/jpeg";
                        } else if (fileName.endsWith(".gif")) {
                            imageMimeType = "image/gif";
                        } else if (fileName.endsWith(".webp")) {
                            imageMimeType = "image/webp";
                        } else {
                            imageMimeType = "image/jpeg";
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Soru görseli okunamadı: questionId={}, imageUrl={}, error={}",
                            questionId, question.getImageUrl(), ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Soru görseli okunamadı: questionId={}, error={}", questionId, e.getMessage());
        }

        // 6. OpenAI Chat API çağrısı (text response al)
        String aiResponse;
        Integer tokenCount;
        try {
            Map<String, Object> result = callOpenAIChatAPI(context, history, question, base64Image,
                    imageMimeType);
            aiResponse = (String) result.get("content");
            tokenCount = (Integer) result.get("tokenCount");
        } catch (Exception e) {
            log.error("OpenAI API çağrısı başarısız: {}", e.getMessage(), e);
            throw new ExternalServiceException("AI yanıtı alınamadı: " + e.getMessage());
        }

        // 7. AI text yanıtını kaydet
        QuestionAIMessage aiMsg = new QuestionAIMessage();
        aiMsg.setConversation(conversation);
        aiMsg.setContent(aiResponse);
        aiMsg.setRole(MessageRole.ASSISTANT);
        aiMsg.setMessageType(MessageType.ASSISTANT_TEXT); // Text yanıt
        aiMsg.setTokenCount(tokenCount);
        messageRepository.save(aiMsg);

        // 8. Conversation'ı güncelle
        conversation.setLastMessageAt(aiMsg.getCreatedAt());
        conversationRepository.save(conversation);

        // 9. AI text yanıtını audio'ya çevir (TTS)
        String audioUrl = null;
        try {
            byte[] audioBytes = speechProcessingService.textToSpeech(aiResponse);
            audioUrl = speechProcessingService.saveTemporaryAudio(audioBytes);
            log.info("AI yanıtı audio'ya çevrildi: questionId={}, audioUrl={}", questionId, audioUrl);
        } catch (Exception e) {
            log.error("AI yanıtı audio'ya çevrilemedi: questionId={}, error={}", questionId, e.getMessage(), e);
            // Audio çevrilemezse devam et, sadece text response döndür
        }

        // 10. Response oluştur
        AIMessageResponse response = toMessageResponse(aiMsg);
        response.setAudioUrl(audioUrl); // Geçici audio URL (nullable)
        return response;
    }

    /**
     * OpenAI API maliyetini tahmin eder (token bazlı)
     * Pricing (approximate as of 2024):
     * - gpt-4o-mini: $0.15/1M input tokens, $0.60/1M output tokens
     * - gpt-4o: $2.50/1M input tokens, $10.00/1M output tokens
     * - gpt-4-turbo: $10.00/1M input tokens, $30.00/1M output tokens
     * 
     * @param modelName        Model adı
     * @param promptTokens     Prompt token sayısı
     * @param completionTokens Completion token sayısı
     * @return Tahmini maliyet (USD)
     */
    private double estimateAICost(String modelName, int promptTokens, int completionTokens) {
        double inputPricePerMillion;
        double outputPricePerMillion;

        if (modelName.contains("gpt-4o-mini") || modelName.contains("mini")) {
            inputPricePerMillion = 0.15;
            outputPricePerMillion = 0.60;
        } else if (modelName.contains("gpt-4o")) {
            inputPricePerMillion = 2.50;
            outputPricePerMillion = 10.00;
        } else if (modelName.contains("gpt-4-turbo") || modelName.contains("gpt-4")) {
            inputPricePerMillion = 10.00;
            outputPricePerMillion = 30.00;
        } else {
            // Default estimate for unknown models
            inputPricePerMillion = 1.0;
            outputPricePerMillion = 3.0;
        }

        double inputCost = (promptTokens / 1_000_000.0) * inputPricePerMillion;
        double outputCost = (completionTokens / 1_000_000.0) * outputPricePerMillion;

        return inputCost + outputCost;
    }

    /**
     * Message entity'yi Response DTO'ya çevir
     */
    private AIMessageResponse toMessageResponse(QuestionAIMessage message) {
        AIMessageResponse response = new AIMessageResponse();
        response.setId(message.getId());
        response.setContent(message.getContent());
        response.setRole(message.getRole().name());
        response.setMessageType(message.getMessageType() != null ? message.getMessageType().name() : null);
        response.setCreatedAt(message.getCreatedAt());
        // audioUrl sadece response oluşturulurken set edilecek (geçici audio için)
        return response;
    }

    /**
     * OpenAI API için messages listesi oluştur
     */
    private List<Map<String, Object>> buildMessagesForAPI(String context,
            List<QuestionAIMessage> history,
            String base64Image,
            String imageMimeType) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // System message
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", context);
        messages.add(systemMessage);

        // History (son 5 mesaj)
        List<QuestionAIMessage> recentHistory = pickRecentHistory(history, 5);
        boolean shouldSendImage = base64Image != null &&
                (recentHistory.isEmpty() ||
                        (recentHistory.size() == 1 && recentHistory.get(0).getRole() == MessageRole.USER));

        boolean imageSent = false;
        for (QuestionAIMessage msg : recentHistory) {
            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("role", msg.getRole().name().toLowerCase());

            if (shouldSendImage && !imageSent && msg.getRole() == MessageRole.USER) {
                // Image + text content
                List<Map<String, Object>> content = new ArrayList<>();

                Map<String, Object> textPart = new HashMap<>();
                textPart.put("type", "text");
                textPart.put("text", msg.getContent());
                content.add(textPart);

                Map<String, Object> imagePart = new HashMap<>();
                imagePart.put("type", "image_url");
                Map<String, String> imageUrl = new HashMap<>();
                String mime = (imageMimeType != null && !imageMimeType.isEmpty())
                        ? imageMimeType
                        : "image/jpeg";
                imageUrl.put("url", "data:" + mime + ";base64," + base64Image);
                imagePart.put("image_url", imageUrl);
                content.add(imagePart);

                msgMap.put("content", content);
                imageSent = true;
            } else {
                msgMap.put("content", msg.getContent());
            }

            messages.add(msgMap);
        }

        return messages;
    }

    /**
     * Soru görselini hazırla (base64 encode)
     */
    private String prepareQuestionImage(Question question) {
        if (question.getImageUrl() == null || question.getImageUrl().trim().isEmpty()) {
            return null;
        }

        try {
            org.springframework.core.io.Resource imageResource = fileService.getFile(question.getImageUrl());

            if (imageResource.exists() && imageResource.isReadable()) {
                byte[] imageBytes = imageResource.getInputStream().readAllBytes();
                return Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (Exception e) {
            log.warn("Soru görseli okunamadı: questionId={}, error={}",
                    question.getId(), e.getMessage());
        }

        return null;
    }

    /**
     * Görsel MIME type'ını tespit et
     */
    private String detectImageMimeType(String imageUrl) {
        if (imageUrl == null)
            return null;

        String fileName = imageUrl.toLowerCase();
        if (fileName.endsWith(".png"))
            return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))
            return "image/jpeg";
        if (fileName.endsWith(".gif"))
            return "image/gif";
        if (fileName.endsWith(".webp"))
            return "image/webp";

        return "image/jpeg"; // Default
    }
}
