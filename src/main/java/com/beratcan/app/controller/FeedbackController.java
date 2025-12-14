package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.request.FeedbackRequest;
import com.beratcan.app.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feedback Controller - Kullanıcı geri bildirimlerini yönetir
 */
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Feedback gönder
     * POST /api/feedback
     * Authentication gereklidir
     */
    @PostMapping
    @RateLimit(requestsPerMinute = 3, keyType = "USER", message = "Çok fazla feedback gönderiliyor, lütfen daha sonra tekrar deneyin.")
    public ResponseEntity<?> sendFeedback(
            @Valid @RequestBody FeedbackRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Feedback göndermek için giriş yapmalısınız."));
        }

        Long userId = Long.parseLong(authentication.getName());
        feedbackService.sendFeedback(userId, request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Geri bildiriminiz başarıyla gönderildi. Teşekkür ederiz!"));
    }
}
