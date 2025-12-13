package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.request.SendAIChatMessageRequest;
import com.beratcan.app.dto.response.AIMessageResponse;
import com.beratcan.app.dto.response.QuestionAIConversationResponse;
import com.beratcan.app.service.QuestionAIChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/questions/{questionId}/ai/chat")
@RequiredArgsConstructor
public class QuestionAIChatController {

    private final QuestionAIChatService chatService;
    private final com.beratcan.app.service.FileService fileService;

    /**
     * Conversation getir veya oluştur
     */
    @GetMapping("/conversation")
    @RateLimit(requestsPerMinute = 30, keyType = "USER", message = "Çok fazla istek gönderiliyor, lütfen daha sonra tekrar deneyin.")
    public ResponseEntity<QuestionAIConversationResponse> getOrCreateConversation(
            @PathVariable Long questionId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        QuestionAIConversationResponse response = chatService.getOrCreateConversation(questionId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Mesaj gönder ve AI yanıtı al (Text)
     */
    @PostMapping("/messages")
    @RateLimit(requestsPerMinute = 20, keyType = "USER", message = "Çok fazla mesaj gönderiliyor, lütfen daha sonra tekrar deneyin.")
    public ResponseEntity<AIMessageResponse> sendMessage(
            @PathVariable Long questionId,
            @RequestBody @Valid SendAIChatMessageRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());

        // Message boş olabilir (voice mesaj için)
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new com.beratcan.app.exception.BadRequestException("Mesaj boş olamaz");
        }

        AIMessageResponse response = chatService.sendMessage(questionId, userId, request.getMessage());
        return ResponseEntity.ok(response);
    }

    /**
     * Voice mesaj gönder ve AI yanıtı al (Audio)
     */
    @PostMapping(value = "/messages/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(requestsPerMinute = 10, keyType = "USER", message = "Çok fazla voice mesaj gönderiliyor, lütfen daha sonra tekrar deneyin.")
    public ResponseEntity<AIMessageResponse> sendVoiceMessage(
            @PathVariable Long questionId,
            @RequestParam("audioFile") MultipartFile audioFile,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        AIMessageResponse response = chatService.sendVoiceMessage(questionId, userId, audioFile);
        return ResponseEntity.ok(response);
    }

    /**
     * Geçici audio dosyasını getir
     * Voice mesaj yanıtları için geçici audio dosyalarını serve eder
     */
    @GetMapping("/audio/temp/{filename:.+}")
    public ResponseEntity<Resource> getTemporaryAudio(
            @PathVariable String filename) {
        // Güvenlik: Path traversal önleme
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new com.beratcan.app.exception.BadRequestException("Geçersiz dosya adı");
        }

        // Audio temp dizininden dosyayı al
        String audioPath = "audio/temp/" + filename;
        Resource resource = fileService.getFile(audioPath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

}
