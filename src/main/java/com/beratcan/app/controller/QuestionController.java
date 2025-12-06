package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.request.CreateQuestionRequest;
import com.beratcan.app.dto.request.UpdateAnalysisRequest;
import com.beratcan.app.dto.request.UpdateQuestionRequest;
import com.beratcan.app.dto.response.QuestionResponse;
import com.beratcan.app.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    // CREATE
    /**
     * Soru oluştur (görsel + temel bilgiler) + Otomatik AI analiz
     */
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(requestsPerMinute = 10, keyType = "USER", message = "Too many question creation requests. Please try again later.")
    public QuestionResponse addQuestion(
            @Valid @ModelAttribute CreateQuestionRequest request) {

        return questionService.addQuestionWithImageAndAI(request);
    }

    // READ
    @GetMapping("/by-test/{testId}")
    public List<QuestionResponse> getQuestionsByTest(@PathVariable Long testId) {
        return questionService.getQuestionsByTest(testId);
    }

    @GetMapping("/{questionId}")
    public QuestionResponse getQuestionById(@PathVariable Long questionId) {
        return questionService.getQuestionById(questionId);
    }

    // UPDATE
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{questionId}")
    public QuestionResponse updateQuestion(@PathVariable Long questionId,
            @Valid @ModelAttribute UpdateQuestionRequest request) {
        return questionService.updateQuestion(questionId, request);
    }

    // DELETE
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{questionId}")
    public void deleteQuestion(@PathVariable Long questionId) {
        questionService.deleteQuestion(questionId);
    }

    // Hint için oy verme
    @PostMapping("/{questionId}/hints/{hintIndex}/vote")
    public ResponseEntity<String> voteHint(
            @PathVariable Long questionId,
            @PathVariable int hintIndex,
            @RequestParam boolean like,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("You must be authenticated to vote");
        }
        questionService.voteHint(questionId, hintIndex, like);
        return ResponseEntity.ok("Vote recorded for hint");
    }

    // Explanation için oy verme
    @PostMapping("/{questionId}/explanation/vote")
    public ResponseEntity<String> voteExplanation(
            @PathVariable Long questionId,
            @RequestParam boolean like,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("You must be authenticated to vote");
        }
        questionService.voteExplanation(questionId, like);
        return ResponseEntity.ok("Vote recorded for explanation");
    }

    /**
     * Soru için hints ve explanation'ı birlikte güncelle
     */
    @PutMapping("/{questionId}/analysis")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateAnalysis(
            @PathVariable Long questionId,
            @RequestBody UpdateAnalysisRequest request) {

        questionService.updateAnalysis(questionId, request.getHints(), request.getExplanation());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Analysis updated successfully"));
    }
}
