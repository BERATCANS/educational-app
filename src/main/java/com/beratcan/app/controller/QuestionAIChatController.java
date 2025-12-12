package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.request.SendAIChatMessageRequest;
import com.beratcan.app.dto.response.AIMessageResponse;
import com.beratcan.app.dto.response.QuestionAIConversationResponse;
import com.beratcan.app.service.QuestionAIChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * Mesaj gönder ve AI yanıtı al
     */
    @PostMapping("/messages")
    @RateLimit(requestsPerMinute = 20, keyType = "USER", message = "Çok fazla mesaj gönderiliyor, lütfen daha sonra tekrar deneyin.")
    public ResponseEntity<AIMessageResponse> sendMessage(
            @PathVariable Long questionId,
            @RequestBody @Valid SendAIChatMessageRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        AIMessageResponse response = chatService.sendMessage(questionId, userId, request.getMessage());
        return ResponseEntity.ok(response);
    }

}
