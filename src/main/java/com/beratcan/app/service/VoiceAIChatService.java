package com.beratcan.app.service;

import com.beratcan.app.dto.response.AIMessageResponse;
import com.beratcan.app.entity.*;
import com.beratcan.app.entity.enums.MessageRole;
import com.beratcan.app.entity.enums.MessageType;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.DailyLimitExceededException;
import com.beratcan.app.exception.ExternalServiceException;
import com.beratcan.app.exception.QuestionNotFoundException;
import com.beratcan.app.exception.UserNotFoundException;
import com.beratcan.app.repository.QuestionAIConversationRepository;
import com.beratcan.app.repository.QuestionAIMessageRepository;
import com.beratcan.app.repository.QuestionRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;

/**
 * Sesli AI Chat Servisi
 * 
 * Text AI chat'ten tamamen ayrı.
 * Konuşma formatında prompt kullanır (LaTeX/Markdown yok).
 * STT (Whisper) → GPT (konuşma prompt) → TTS (OpenAI TTS) akışını yönetir.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceAIChatService {

    private final QuestionAIConversationRepository conversationRepository;
    private final QuestionAIMessageRepository messageRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final SpeechProcessingService speechProcessingService;
    private final DailyLimitService dailyLimitService;
    private final AIChatHelper aiChatHelper;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Value("${ai.chat.response.max.words:200}")
    private int responseMaxWords;

    /**
     * Voice mesaj gönder ve AI yanıtı al (audio olarak)
     * 
     * Akış: Audio → STT → GPT (konuşma prompt) → TTS → Audio yanıt
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
        userMsg.setMessageType(MessageType.VOICE_INPUT);
        messageRepository.save(userMsg);

        // 3. Conversation history al
        List<QuestionAIMessage> history = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        // 4. Konuşma formatında context oluştur (LaTeX/Markdown YOK)
        String context = buildVoiceChatContext(question, history);

        // 5. Soru görselini hazırla
        String base64Image = aiChatHelper.prepareQuestionImage(question);
        String imageMimeType = aiChatHelper.detectImageMimeType(question.getImageUrl());

        // 6. OpenAI Chat API çağrısı (konuşma formatında yanıt)
        String aiResponse;
        Integer tokenCount;
        try {
            Map<String, Object> result = aiChatHelper.callOpenAIChatAPI(context, history,
                    base64Image, imageMimeType, "voice");
            aiResponse = (String) result.get("content");
            tokenCount = (Integer) result.get("tokenCount");
        } catch (Exception e) {
            log.error("OpenAI API çağrısı başarısız: {}", e.getMessage(), e);
            throw new ExternalServiceException("AI yanıtı alınamadı: " + e.getMessage());
        }

        // 7. AI yanıtını kaydet (ASSISTANT_VOICE tipi)
        QuestionAIMessage aiMsg = new QuestionAIMessage();
        aiMsg.setConversation(conversation);
        aiMsg.setContent(aiResponse);
        aiMsg.setRole(MessageRole.ASSISTANT);
        aiMsg.setMessageType(MessageType.ASSISTANT_VOICE);
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
        }

        // 10. Response oluştur
        AIMessageResponse response = aiChatHelper.toMessageResponse(aiMsg);
        response.setAudioUrl(audioUrl);
        return response;
    }

    /**
     * Voice mesaj gönder ve AI yanıtı STREAM al (SSE)
     *
     * Akış: Audio → STT → GPT (Stream) → Cümle yakala → TTS → SSE (Audio Chunk)
     */
    @Transactional
    public SseEmitter sendVoiceMessageStream(Long questionId, Long userId, MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BadRequestException("Audio dosyası boş olamaz");
        }

        // 1. Emitter'ı en başta oluştur
        SseEmitter emitter = new SseEmitter(180_000L); // 3 dk timeout

        // 2. Günlük Limit Kontrolü (Hata alınırsa SSE error gönderiliyor)
        try {
            dailyLimitService.checkDailyAiConversationLimit(userId);
        } catch (DailyLimitExceededException e) {
            log.warn("Günlük limit aşıldı, SSE error gönderiliyor: userId={}", userId);
            sendErrorEvent(emitter, e.getMessage(), "DAILY_LIMIT_EXCEEDED");
            return emitter;
        }

        log.info("Voice stream isteği: questionId={}, userId={}", questionId, userId);

        // 3. Audio'yu text'e çevir (Senkron - MultipartFile güvenliği için)
        String userMessageText;
        try {
            userMessageText = speechProcessingService.speechToText(audioFile);
        } catch (Exception e) {
            log.error("STT hatası: {}", e.getMessage());
            // STT hatasını da event olarak gönderebiliriz ama şimdilik exception fırlatıyor
            throw new BadRequestException("Voice mesaj işlenemedi: " + e.getMessage());
        }

        if (userMessageText == null || userMessageText.trim().isEmpty()) {
            throw new BadRequestException("Ses anlaşılamadı");
        }

        // 4. Veritabanı işlemleri (User msg kaydet)
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

        QuestionAIMessage userMsg = new QuestionAIMessage();
        userMsg.setConversation(conversation);
        userMsg.setContent(userMessageText.trim());
        userMsg.setRole(MessageRole.USER);
        userMsg.setMessageType(MessageType.VOICE_INPUT);
        messageRepository.save(userMsg);

        // History al
        List<QuestionAIMessage> history = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        // Context ve Görsel Hazırlığı
        String context = buildVoiceChatContext(question, history);
        String base64Image = aiChatHelper.prepareQuestionImage(question);
        String imageMimeType = aiChatHelper.detectImageMimeType(question.getImageUrl());

        // 5. User Transcript gönder ve Async İşlemi Başlat
        try {
            Map<String, Object> userData = new HashMap<>();
            userData.put("type", "user_transcript");
            userData.put("text", userMessageText);
            emitter.send(SseEmitter.event().name("user_transcript").data(userData));
        } catch (IOException e) {
            log.error("User transcript gönderilemedi: {}", e.getMessage());
        }

        // Async Streaming...

        executorService.submit(() -> {
            try {
                // OpenAI Stream Çağrısı
                Map<String, Object> result = aiChatHelper.streamOpenAIChatAPI(
                        context, history, base64Image, imageMimeType, "voice-stream",
                        (index, sentence) -> {
                            // Her cümle için callback
                            try {
                                // TTS
                                byte[] audioBytes = speechProcessingService.textToSpeech(sentence);
                                String audioUrl = speechProcessingService.saveTemporaryAudio(audioBytes);

                                // SSE Event Gönder: "audio_chunk"
                                Map<String, Object> chunkData = new HashMap<>();
                                chunkData.put("type", "audio_chunk");
                                chunkData.put("index", index);
                                chunkData.put("text", sentence);
                                chunkData.put("audioUrl", audioUrl);

                                emitter.send(SseEmitter.event().name("audio_chunk").data(chunkData));

                            } catch (Exception e) {
                                log.error("Stream sırasında TTS hatası: {}", e.getMessage());
                                // Bir chunk hatası tüm akışı bozmasın, loglayıp devam et
                            }
                        }
                );

                // Stream bitti, sonucu kaydet
                String fullContent = (String) result.get("content");
                Integer tokenCount = (Integer) result.get("tokenCount");

                QuestionAIMessage aiMsg = new QuestionAIMessage();
                aiMsg.setConversation(conversation);
                aiMsg.setContent(fullContent);
                aiMsg.setRole(MessageRole.ASSISTANT);
                aiMsg.setMessageType(MessageType.ASSISTANT_VOICE);
                aiMsg.setTokenCount(tokenCount);
                messageRepository.save(aiMsg);

                conversation.setLastMessageAt(aiMsg.getCreatedAt());
                conversationRepository.save(conversation);

                // Bitiş Eventi: "done"
                Map<String, Object> doneData = new HashMap<>();
                doneData.put("type", "done");
                doneData.put("messageId", aiMsg.getId());
                doneData.put("fullText", fullContent);
                
                emitter.send(SseEmitter.event().name("done").data(doneData));
                emitter.complete();

            } catch (Exception e) {
                log.error("Voice streaming hatası: {}", e.getMessage(), e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    // =====================================================
    // Context / Prompt
    // =====================================================

    /**
     * Sesli chat için konuşma formatında context oluştur.
     * LaTeX ve Markdown KULLANILMAZ.
     */
    private String buildVoiceChatContext(Question question, List<QuestionAIMessage> history) {
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

        // Rol ve görev tanımı (sesli moda özel)
        context.append("=== ROL VE GÖREV ===\n");
        context.append("Sen bir matematik öğretmenisin. Öğrenciyle SESLİ konuşuyorsun.\n");
        context.append("Yanıtların doğrudan bir TTS motoruna verilecek ve SESLİ OKUNACAK.\n");
        context.append("Bu yüzden yanıtın tamamen düz metin olmalı. Hiçbir biçimlendirme, sembol veya özel karakter KULLANMA.\n");
        context.append("Parantez, yıldız, diyez, tire listesi, numaralı liste, bold, italic gibi HİÇBİR şey kullanma.\n");
        context.append("Amacın öğrencinin soruyu anlamasını ve çözmesini sağlamak.\n");
        context.append("Öğrencinin her sorusuna doğrudan, net ve anlaşılır şekilde yanıt ver.\n\n");

        // Öğretim prensipleri
        context.append("=== ÖĞRETİM PRENSİPLERİ ===\n");
        context.append("1. Sokratik yöntem kullan: Öğrenciyi düşündür, gerektiğinde rehberlik et\n");
        context.append("2. Esnek yaklaşım: Öğrencinin sorusuna göre gerekirse detaylı, gerekirse kısa açıkla\n");
        context.append("3. Örneklerle açıkla: Soyut kavramları somut örneklerle göster\n");
        context.append("4. Pozitif ve destekleyici ol: Öğrenciyi cesaretlendir\n");
        context.append("5. Hataları öğrenme fırsatı olarak gör: Yapıcı geri bildirim ver\n\n");

        // Sesli yanıt kuralları (LaTeX/Markdown YOK)
        context.append("=== SESLİ YANIT KURALLARI ===\n");
        context.append("1. Her zaman Türkçe yanıt ver\n");
        context.append("2. Öğrencinin sorusuna doğrudan odaklan\n");
        context.append("3. ASLA LaTeX, Markdown veya matematiksel sembol kullanma! Hiçbir koşulda $, \\frac, \\sqrt, $$, #, **, ```, * gibi işaretler kullanma.\n");
        context.append("4. Matematiksel ifadeleri KONUŞMA DİLİNDE yaz:\n");
        context.append("   - 'x eşittir 5' ($x = 5$ yerine)\n");
        context.append("   - 'a bölü b' (a/b yerine)\n");
        context.append("   - 'x üzeri 2' veya 'x kare' (x² yerine)\n");
        context.append("   - 'kök x' veya 'x in karekökü' (√x yerine)\n");
        context.append("   - 'eksi b artı eksi kök delta bölü 2a' (ikinci dereceden denklem formülü için)\n");
        context.append("   - 'toplam' (∑ yerine)\n");
        context.append("   - 'integral' (∫ yerine)\n");
        context.append("   - 'pi sayısı' (π yerine)\n");
        context.append("   - 'sonsuz' (∞ yerine)\n");
        context.append("5. FONKSİYON GÖSTERİMLERİNİ de konuşma dilinde yaz:\n");
        context.append("   - 'f of x' veya 'f x' (f(x) yerine)\n");
        context.append("   - 'f of x artı 2' (f(x+2) yerine)\n");
        context.append("   - 'f of eksi 1 bölü 2' (f(-1/2) yerine)\n");
        context.append("   - 'g of x eşittir...' (g(x) = ... yerine)\n");
        context.append("   - Parantez, virgül gibi işaretleri yazma, konuşma dilinde ifade et\n");
        context.append("6. ADIM ADIM ANLATIRKEN numaralı liste (1. 2. 3.) KULLANMA:\n");
        context.append("   - Bunun yerine doğal geçişler kullan: 'İlk olarak...', 'Şimdi...', 'Sonra...', 'Son olarak...'\n");
        context.append("   - Konuşma akışı gibi doğal bir anlatım olsun, madde madde değil\n");
        context.append("7. Cümleleri kısa ve net tut, her cümle sesli okunacak\n");
        context.append("8. ASLA biçimlendirme işareti kullanma: yıldız (*), çift yıldız (**), alt çizgi (_), diyez (#), tire listesi (- ), ters tırnak (`) YASAK\n");
        context.append("9. Doğal ve samimi bir konuşma tonu kullan\n");
        context.append("10. Emoji kullanma\n");
        context.append("11. Maksimum ").append(responseMaxWords).append(" kelime içinde kal\n");
        context.append("12. ÖNEMLİ: Yanıtın bir kişinin sesli okuduğunda doğal ve anlaşılır olacak şekilde olmalı. Sanki bir öğretmen karşısındaki öğrenciye konuşuyor gibi yaz.\n\n");

        // Son hatırlatma - en güçlü vurgu
        context.append("=== KESİN YASAKLAR (İHLAL ETME!) ===\n");
        context.append("YASAK: f(x), g(x), f(x+2) gibi parantezli fonksiyon gösterimi. Bunun yerine 'f x', 'g x', 'f x artı 2' yaz.\n");
        context.append("YASAK: ** veya * ile kalın/italik yazı. Düz metin yaz.\n");
        context.append("YASAK: 1. 2. 3. ile numaralı liste. Bunun yerine 'İlk olarak', 'Sonra', 'Son olarak' gibi geçişler kullan.\n");
        context.append("YASAK: - ile tire listesi.\n");
        context.append("YASAK: Herhangi bir özel karakter (#, `, _, ~, |, >, [, ]).\n");
        context.append("Yanıtın tamamen DÜZ METİN olmalı, sanki birine sesli konuşuyormuşsun gibi.\n\n");

        return context.toString();
    }

    private void sendErrorEvent(SseEmitter emitter, String message, String code) {
        try {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("message", message);
            errorData.put("code", code);
            errorData.put("type", "error");
            emitter.send(SseEmitter.event().name("error").data(errorData));
            emitter.complete();
        } catch (IOException ex) {
            log.error("Error event gönderilemedi: {}", ex.getMessage());
        }
    }
}
