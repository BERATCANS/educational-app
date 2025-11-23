package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.response.FlaggedResponse;
import com.beratcan.app.service.QuestionFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions/flagged")
@RequiredArgsConstructor
public class QuestionFlagController {

    private final QuestionFlagService questionFlagService;

    @PostMapping("/{questionId}")
    @RateLimit(requestsPerMinute = 10, keyType = "USER", message = "Too many flag submissions. Please try again later.")
    public ResponseEntity<?> flagQuestion(
            @PathVariable Long questionId,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Soruyu işaretlemek için giriş yapmalısınız."));
        }
        Long userId = Long.parseLong(authentication.getName());
        questionFlagService.flagQuestion(userId, questionId);
        return ResponseEntity.ok(Map.of("message", "Question flagged successfully."));
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<?> unflagQuestion(
            @PathVariable Long questionId,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Soruyu işaretten kaldırmak için giriş yapmalısınız."));
        }
        Long userId = Long.parseLong(authentication.getName());
        questionFlagService.unflagQuestion(userId, questionId);
        return ResponseEntity.ok(Map.of("message", "Question unflagged successfully."));
    }

    @GetMapping
    public ResponseEntity<?> getFlaggedQuestions(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "İşaretli soruları görmek için giriş yapmalısınız."));
        }
        Long userId = Long.parseLong(authentication.getName());
        List<FlaggedResponse> response = questionFlagService.getFlaggedQuestions(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<?> getFlaggedQuestionById(
            @PathVariable Long questionId,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Soruyu görmek için giriş yapmalısınız."));
        }
        Long userId = Long.parseLong(authentication.getName());
        // Service'de ownership kontrolü yapılıyor - sadece kullanıcının işaretlediği
        // soruları görebilir
        FlaggedResponse response = questionFlagService.getFlaggedQuestionById(userId, questionId);
        return ResponseEntity.ok(response);
    }
}
