package com.beratcan.app.controller;

import com.beratcan.app.dto.response.FlaggedResponse;
import com.beratcan.app.service.QuestionFlagService;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<?> flagQuestion(
            @PathVariable Long questionId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        questionFlagService.flagQuestion(userId, questionId);
        return ResponseEntity.ok(Map.of("message", "Question flagged successfully."));
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<?> unflagQuestion(
            @PathVariable Long questionId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        questionFlagService.unflagQuestion(userId, questionId);
        return ResponseEntity.ok(Map.of("message", "Question unflagged successfully."));
    }

    @GetMapping
    public ResponseEntity<List<FlaggedResponse>> getFlaggedQuestions(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<FlaggedResponse> response = questionFlagService.getFlaggedQuestions(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<FlaggedResponse> getFlaggedQuestionById(@PathVariable Long questionId) {
        FlaggedResponse response = questionFlagService.getFlaggedQuestionById(questionId);
        return ResponseEntity.ok(response);
    }
}
