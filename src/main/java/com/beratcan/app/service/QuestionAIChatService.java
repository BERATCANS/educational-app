package com.beratcan.app.service;

import com.beratcan.app.constants.AppConstants;
import com.beratcan.app.dto.response.AIMessageResponse;
import com.beratcan.app.dto.response.QuestionAIConversationResponse;
import com.beratcan.app.entity.*;
import com.beratcan.app.entity.enums.MessageRole;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
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
    private final ObjectMapper objectMapper = new ObjectMapper();
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
                // imageUrl'den "api/files/" veya "/api/files/" prefix'ini temizle (URL
                // formatıysa)
                // Database'de filename olarak saklanıyor ama bazı kayıtlarda URL formatında
                // olabilir
                String imageFilename = question.getImageUrl().trim();
                if (imageFilename.startsWith("/api/files/")) {
                    imageFilename = imageFilename.substring("/api/files/".length());
                } else if (imageFilename.startsWith("api/files/")) {
                    imageFilename = imageFilename.substring("api/files/".length());
                }

                // uploadDir'in sonunda / var mı kontrol et ve dosya yolunu oluştur
                String separator = uploadDir.endsWith("/") || uploadDir.endsWith("\\") ? "" : "/";
                File imageFile = new File(uploadDir + separator + imageFilename);

                log.debug(
                        "Görsel dosyası aranıyor: questionId={}, originalImageUrl={}, cleanedFilename={}, fullPath={}",
                        questionId, question.getImageUrl(), imageFilename, imageFile.getAbsolutePath());

                if (imageFile.exists() && imageFile.isFile()) {
                    byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
                    base64Image = Base64.getEncoder().encodeToString(imageBytes);

                    // Dosya uzantısına göre MIME type belirle
                    String fileName = imageFile.getName().toLowerCase();
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
                    log.warn(
                            "Soru görseli bulunamadı: questionId={}, originalImageUrl={}, cleanedFilename={}, fullPath={}, exists={}",
                            questionId, question.getImageUrl(), imageFilename, imageFile.getAbsolutePath(),
                            imageFile.exists());
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
            Map<String, Object> result = callOpenAIChatAPI(context, history, userMessage, base64Image, imageMimeType);
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
        context.append("Amacın öğrencinin soruyu anlamasını ve çözmesini sağlamak.\n\n");

        // Öğretim prensipleri
        context.append("=== ÖĞRETİM PRENSİPLERİ ===\n");
        context.append("1. Sokratik yöntem kullan: Öğrenciyi düşündür, doğrudan cevap verme\n");
        context.append("2. Adım adım ilerle: Her adımı açıkla ve öğrencinin takip etmesini sağla\n");
        context.append("3. Örneklerle açıkla: Soyut kavramları somut örneklerle göster\n");
        context.append("4. Pozitif ve destekleyici ol: Öğrenciyi cesaretlendir\n");
        context.append("5. Hataları öğrenme fırsatı olarak gör: Yapıcı geri bildirim ver\n\n");

        // Yanıt kuralları
        context.append("=== YANIT KURALLARI ===\n");
        context.append("1. Her zaman Türkçe yanıt ver\n");
        context.append("2. Kısa ve öz ol (maksimum ").append(responseMaxWords).append(" kelime)\n");
        context.append("3. Formülleri açıkla: Sadece yazma, ne anlama geldiğini de söyle\n");
        context.append("4. Öğrencinin seviyesine uygun dil kullan\n");
        context.append("5. Emoji kullanma, profesyonel kal\n\n");

        // Özel durumlar - AI kendi karar versin
        context.append("=== ÖZEL DURUMLAR ===\n");
        context.append("Öğrenci ile konuşurken şu durumları dikkate al:\n");
        context.append("- Öğrenci doğru cevabı isterse (açıkça söylerse) verebilirsin\n");
        context.append("- Hata türlerini (işlem hatası, konu hatası) kendin algıla ve ona göre yaklaş\n");
        context.append("- Konu dışı soruları kendin tespit et ve uygun şekilde yanıtla\n");
        context.append("- Öğrenci anlamadıysa daha basit dil kullan, farklı yöntemler dene\n");
        context.append("- Doğru cevap bulunca tebrik et, konuyu pekiştir, benzer örnekler ver\n\n");

        // İpucu kullanım stratejisi
        context.append("=== İPUCU KULLANIM STRATEJİSİ ===\n");
        context.append("1. Önce mevcut ipuçlarını kullan (yukarıda listelenen)\n");
        context.append("2. Öğrenci zorlanırsa yeni ipuçları üret\n");
        context.append("3. İpuçları kademeli olarak daha spesifik olmalı\n\n");

        // Yanıt formatı
        context.append("=== YANIT FORMATI ===\n");
        context.append("Yanıtını şu yapıda ver:\n");
        context.append("1. Öğrencinin mesajını anladığını göster (kısa)\n");
        context.append("2. Sorunun/hatanın ne olduğunu açıkla\n");
        context.append("3. Çözüm yolunu adım adım göster\n");
        context.append("4. Öğrenciyi bir sonraki adıma yönlendir\n\n");

        // Conversation history notu
        context.append("=== CONVERSATION HISTORY ===\n");
        context.append("Öğrenci ile olan tüm konuşma geçmişi sana gönderilecek.\n");
        context.append("Bu geçmişi kullanarak öğrencinin hangi aşamada olduğunu anla ve ona göre yanıt ver.\n");
        context.append("Öğrencinin daha önce sorduğu soruları ve verdiğin yanıtları dikkate al.\n");

        return context.toString();
    }

    /**
     * OpenAI Chat Completions API çağrısı
     */
    private Map<String, Object> callOpenAIChatAPI(String context, List<QuestionAIMessage> history, String userMessage,
            String base64Image, String imageMimeType) {
        long startTime = System.currentTimeMillis();

        try {
            // Messages listesi oluştur
            List<Map<String, Object>> messages = new ArrayList<>();

            // System message (context)
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", context);
            messages.add(systemMessage);

            // Conversation history (son 10 mesaj - token limiti için)
            List<QuestionAIMessage> recentHistory = history.size() > 10
                    ? history.subList(history.size() - 10, history.size())
                    : history;

            for (QuestionAIMessage msg : recentHistory) {
                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("role", msg.getRole().name().toLowerCase());
                msgMap.put("content", msg.getContent());
                messages.add(msgMap);
            }

            // Yeni kullanıcı mesajı (görsel varsa image_url ile birlikte)
            Map<String, Object> newUserMessage = new HashMap<>();
            newUserMessage.put("role", "user");

            if (base64Image != null) {
                // Görsel varsa content array olarak gönder
                List<Map<String, Object>> content = new ArrayList<>();

                // Text part
                Map<String, Object> textPart = new HashMap<>();
                textPart.put("type", "text");
                textPart.put("text", userMessage);
                content.add(textPart);

                // Image part
                Map<String, Object> imagePart = new HashMap<>();
                imagePart.put("type", "image_url");
                Map<String, String> imageUrl = new HashMap<>();

                // MIME type'a göre doğru format kullan
                if (imageMimeType != null && !imageMimeType.isEmpty()) {
                    imageUrl.put("url", "data:" + imageMimeType + ";base64," + base64Image);
                } else {
                    // Fallback: varsayılan olarak JPEG (çoğu görsel JPEG)
                    imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
                    log.warn("MIME type belirlenemedi, JPEG olarak gönderiliyor");
                }

                imagePart.put("image_url", imageUrl);
                content.add(imagePart);

                newUserMessage.put("content", content);
            } else {
                // Görsel yoksa sadece text
                newUserMessage.put("content", userMessage);
            }
            messages.add(newUserMessage);

            // Request body
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("model", model);
            requestMap.put("messages", messages);
            requestMap.put("max_tokens", 1000);
            requestMap.put("temperature", chatTemperature);

            String requestBody = objectMapper.writeValueAsString(requestMap);

            log.info("Calling OpenAI Chat API: model={}, messages={}, hasImage={}",
                    model, messages.size(), base64Image != null);

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
                if (jsonResponse.has("usage")) {
                    JsonNode usage = jsonResponse.get("usage");
                    if (usage.has("total_tokens")) {
                        tokenCount = usage.get("total_tokens").asInt();
                    }
                }

                log.info("OpenAI API success: duration={}ms, responseLength={} chars, tokens={}",
                        duration, content.length(), tokenCount);

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
     * Message entity'yi Response DTO'ya çevir
     */
    private AIMessageResponse toMessageResponse(QuestionAIMessage message) {
        AIMessageResponse response = new AIMessageResponse();
        response.setId(message.getId());
        response.setContent(message.getContent());
        response.setRole(message.getRole().name());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }
}
