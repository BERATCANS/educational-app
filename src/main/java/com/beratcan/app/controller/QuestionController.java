package com.beratcan.app.controller;

import com.beratcan.app.dto.request.QuestionRequest;
import com.beratcan.app.dto.response.QuestionResponse;
import com.beratcan.app.service.QuestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    // CREATE
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public QuestionResponse addQuestion(@RequestParam Long testId, @RequestBody QuestionRequest request) {
        return questionService.addQuestion(testId, request);
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

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/flagged")
    public List<QuestionResponse> getFlaggedQuestions() {
        return questionService.getFlaggedQuestions();
    }

    // UPDATE
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{questionId}")
    public QuestionResponse updateQuestion(@PathVariable Long questionId,
            @RequestBody QuestionRequest request) {
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
}
