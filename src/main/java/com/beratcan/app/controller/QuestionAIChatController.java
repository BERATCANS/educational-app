package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.request.SendAIChatMessageRequest;
import com.beratcan.app.dto.response.AIMessageResponse;
import com.beratcan.app.dto.response.QuestionAIConversationResponse;
import com.beratcan.app.service.QuestionAIChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.beratcan.app.exception.BadRequestException;


@Slf4j
@RestController
@RequestMapping("/api/questions/{questionId}/ai/chat")
@RequiredArgsConstructor
public class QuestionAIChatController {

    private final QuestionAIChatService chatService;

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
            throw new BadRequestException("Mesaj boş olamaz");
        }

        AIMessageResponse response = chatService.sendMessage(questionId, userId, request.getMessage());
        return ResponseEntity.ok(response);
    }

    /**
     * Mesaj gönder ve AI yanıtı al (Streaming - SSE)
     */
    @PostMapping("/messages/stream")
    @RateLimit(requestsPerMinute = 20, keyType = "USER", message = "Çok fazla mesaj gönderiliyor, lütfen daha sonra tekrar deneyin.")
    public SseEmitter sendMessageStream(
            @PathVariable Long questionId,
            @RequestBody @Valid SendAIChatMessageRequest request) {

        // Authentication'ı manuel al (SseEmitter ile parameter injection sorunlu
        // olabiliyor)
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication();

        log.info("🔍 Streaming endpoint called: questionId={}, auth={}, authenticated={}, name={}",
                questionId,
                authentication != null,
                authentication != null && authentication.isAuthenticated(),
                authentication != null ? authentication.getName() : "null");

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            log.error("❌ Authentication failed in streaming endpoint");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Long userId = Long.parseLong(authentication.getName());

        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new BadRequestException("Mesaj boş olamaz");
        }

        SseEmitter emitter = new SseEmitter(30000L); // 30 saniye timeout
        chatService.sendMessageStream(questionId, userId, request.getMessage(), emitter);

        return emitter;
    }

}
