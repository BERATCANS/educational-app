package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.request.DailyQuestionAnswerRequest;
import com.beratcan.app.dto.request.CreateDailyQuestionRequest;
import com.beratcan.app.dto.response.DailyQuestionResponse;
import com.beratcan.app.entity.DailyQuestion;
import com.beratcan.app.service.DailyQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/daily-questions")
@RequiredArgsConstructor
public class DailyQuestionController {

    private final DailyQuestionService dailyQuestionService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<?> createDailyQuestion(
            @Valid @ModelAttribute CreateDailyQuestionRequest request) {
        DailyQuestion question = dailyQuestionService.createDailyQuestion(request);
        return ResponseEntity.ok(new DailyQuestionResponse(question, null));
    }

    @GetMapping("/current")
    public ResponseEntity<DailyQuestionResponse> getCurrentQuestion(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        DailyQuestionResponse response = dailyQuestionService.getCurrentQuestion(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/answer")
    @RateLimit(requestsPerMinute = 3, keyType = "USER", message = "Too many answer submissions. Please try again later.")
    public ResponseEntity<DailyQuestionResponse> submitAnswer(
            @Valid @RequestBody DailyQuestionAnswerRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        DailyQuestionResponse response = dailyQuestionService.submitAnswer(userId, request);
        return ResponseEntity.ok(response);
    }
}