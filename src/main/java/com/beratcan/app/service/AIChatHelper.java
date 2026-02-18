package com.beratcan.app.service;

import com.beratcan.app.constants.AppConstants;
import com.beratcan.app.dto.response.AIMessageResponse;
import com.beratcan.app.entity.Question;
import com.beratcan.app.entity.QuestionAIMessage;
import com.beratcan.app.entity.enums.MessageRole;
import com.beratcan.app.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * AI Chat ortak helper metotları.
 * 
 * QuestionAIChatService (text) ve VoiceAIChatService (sesli) tarafından
 * paylaşılan yardımcı metotları içerir: OpenAI API çağrıları, mesaj hazırlama,
 * görsel işleme, DTO dönüşümleri.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AIChatHelper {

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

    // =====================================================
    // OpenAI API
    // =====================================================

    /**
     * OpenAI Chat Completions API çağrısı (non-streaming)
     *
     * @param context       System prompt (text veya voice context)
     * @param history       Conversation history
     * @param base64Image   Soru görseli (nullable)
     * @param imageMimeType Görsel MIME type (nullable)
     * @param caller        Log prefix (örn: "text", "voice")
     * @return Map: "content" → AI yanıtı, "tokenCount" → toplam token
     */
    public Map<String, Object> callOpenAIChatAPI(String context, List<QuestionAIMessage> history,
            String base64Image, String imageMimeType, String caller) {
        long startTime = System.currentTimeMillis();

        try {
            List<Map<String, Object>> messages = buildMessagesForAPI(context, history,
                    base64Image, imageMimeType);

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("model", model);
            requestMap.put("messages", messages);
            requestMap.put("max_tokens", 1000);
            requestMap.put("temperature", chatTemperature);

            String requestBody = objectMapper.writeValueAsString(requestMap);

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                long duration = System.currentTimeMillis() - startTime;

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    log.error("OpenAI API failed ({}): status={}, duration={}ms, error={}",
                            caller, response.code(), duration, errorBody);
                    throw new ExternalServiceException(
                            String.format("OpenAI API hatası: HTTP %d - %s", response.code(), errorBody));
                }

                String responseBody = response.body().string();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);

                String content = jsonResponse.get("choices").get(0).get("message").get("content").asText();

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

                double estimatedCost = estimateAICost(model, promptTokens != null ? promptTokens : 0,
                        completionTokens != null ? completionTokens : 0);

                log.info("OpenAI API call ({}): model={} | duration={}ms | tokens={} (prompt={}, completion={}) | estimatedCost=${}",
                        caller, model, duration, tokenCount != null ? tokenCount : 0,
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
            log.error("OpenAI API çağrısı sırasında hata ({}): {}", caller, e.getMessage(), e);
            throw new ExternalServiceException("OpenAI API çağrısı başarısız: " + e.getMessage(), e);
        }
    }

    // =====================================================
    // OpenAI API Streaming
    // =====================================================

    /**
     * OpenAI Chat Completions API çağrısı (STREAMING)
     * Yanıtı cümle cümle stream eder.
     *
     * @param context       System prompt
     * @param history       Conversation history
     * @param base64Image   Soru görseli (nullable)
     * @param imageMimeType Görsel MIME type (nullable)
     * @param caller        Log prefix
     * @param onSentence    Her cümle tamamlandığında çağrılacak callback (index, sentence)
     * @return Map: "content" → Full AI yanıtı, "tokenCount" → estimated token count
     */
    public Map<String, Object> streamOpenAIChatAPI(String context, List<QuestionAIMessage> history,
                                                   String base64Image, String imageMimeType, String caller,
                                                   java.util.function.BiConsumer<Integer, String> onSentence) {
        long startTime = System.currentTimeMillis();
        StringBuilder fullContent = new StringBuilder();
        StringBuilder sentenceBuffer = new StringBuilder();
        int sentenceIndex = 0;

        try {
            List<Map<String, Object>> messages = buildMessagesForAPI(context, history,
                    base64Image, imageMimeType);

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("model", model);
            requestMap.put("messages", messages);
            requestMap.put("max_tokens", 1000);
            requestMap.put("temperature", chatTemperature);
            requestMap.put("stream", true); // Streaming aktif

            String requestBody = objectMapper.writeValueAsString(requestMap);

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    log.error("OpenAI API Streaming failed ({}): status={}, error={}",
                            caller, response.code(), errorBody);
                    throw new ExternalServiceException(
                            String.format("OpenAI API hatası: HTTP %d - %s", response.code(), errorBody));
                }

                // Stream okuma
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(response.body().byteStream()));
                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;

                        try {
                            JsonNode node = objectMapper.readTree(data);
                            if (node.has("choices") && node.get("choices").isArray() && !node.get("choices").isEmpty()) {
                                JsonNode choice = node.get("choices").get(0);
                                if (choice.has("delta") && choice.get("delta").has("content")) {
                                    String contentChunk = choice.get("delta").get("content").asText();
                                    if (contentChunk != null && !contentChunk.isEmpty()) {
                                        fullContent.append(contentChunk);
                                        sentenceBuffer.append(contentChunk);

                                        // Cümle sonu kontrolü (. ? ! \n)
                                        // Basit bir regex ile kontrol ediyoruz, geliştirilebilir
                                        if (contentChunk.matches(".*[.?!\\n]")) {
                                            String text = sentenceBuffer.toString().trim();
                                            if (text.length() > 2) { // Çok kısa parçaları atlamak için (örn: "1.")
                                                onSentence.accept(sentenceIndex++, text);
                                                sentenceBuffer.setLength(0); // Buffer temizle
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Stream line parse fail: {}", line);
                        }
                    }
                }

                // Kalan buffer varsa son cümle olarak gönder
                if (sentenceBuffer.length() > 0) {
                    String text = sentenceBuffer.toString().trim();
                    if (!text.isEmpty()) {
                        onSentence.accept(sentenceIndex++, text);
                    }
                }

                long duration = System.currentTimeMillis() - startTime;
                String finalContent = fullContent.toString();
                
                // Streaming modunda token usage gelmeyebilir, tahmin ediyoruz
                // (OpenAI bazen stream sonunda usage chunk atıyor ama garantili değil)
                int promptTokens = (context.length() + history.toString().length()) / 4; // Kaba tahmin
                int completionTokens = finalContent.length() / 4;
                int totalTokens = promptTokens + completionTokens;
                
                double estimatedCost = estimateAICost(model, promptTokens, completionTokens);

                log.info("OpenAI Streaming ({}): model={} | duration={}ms | approxTokens={} | estimatedCost=${}",
                        caller, model, duration, totalTokens, String.format("%.4f", estimatedCost));

                Map<String, Object> result = new HashMap<>();
                result.put("content", finalContent);
                result.put("tokenCount", totalTokens);
                return result;
            }

        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI Streaming hatası ({}): {}", caller, e.getMessage(), e);
            throw new ExternalServiceException("OpenAI Streaming başarısız: " + e.getMessage(), e);
        }
    }

    // =====================================================
    // Message Building
    // =====================================================

    /**
     * OpenAI API için messages listesi oluştur
     */
    public List<Map<String, Object>> buildMessagesForAPI(String context,
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
     * Son N mesajı al (hem USER hem ASSISTANT)
     */
    public List<QuestionAIMessage> pickRecentHistory(List<QuestionAIMessage> history, int maxMessages) {
        if (history.size() <= maxMessages) {
            return history;
        }
        return history.subList(history.size() - maxMessages, history.size());
    }

    // =====================================================
    // Image Helpers
    // =====================================================

    /**
     * Soru görselini hazırla (base64 encode)
     */
    public String prepareQuestionImage(Question question) {
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
    public String detectImageMimeType(String imageUrl) {
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

        return "image/jpeg";
    }

    // =====================================================
    // DTO Conversion
    // =====================================================

    /**
     * Message entity'yi Response DTO'ya çevir
     */
    public AIMessageResponse toMessageResponse(QuestionAIMessage message) {
        AIMessageResponse response = new AIMessageResponse();
        response.setId(message.getId());
        response.setContent(message.getContent());
        response.setRole(message.getRole().name());
        response.setMessageType(message.getMessageType() != null ? message.getMessageType().name() : null);
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }

    // =====================================================
    // Cost Estimation
    // =====================================================

    /**
     * OpenAI API maliyetini tahmin eder (token bazlı)
     */
    public double estimateAICost(String modelName, int promptTokens, int completionTokens) {
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
            inputPricePerMillion = 1.0;
            outputPricePerMillion = 3.0;
        }

        double inputCost = (promptTokens / 1_000_000.0) * inputPricePerMillion;
        double outputCost = (completionTokens / 1_000_000.0) * outputPricePerMillion;

        return inputCost + outputCost;
    }
}
