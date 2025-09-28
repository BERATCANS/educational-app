package com.beratcan.app.controller;

import com.beratcan.app.dto.request.QuestionRequest;
import com.beratcan.app.dto.response.QuestionResponse;
import com.beratcan.app.service.QuestionService;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/flagged")
    public List<QuestionResponse> getFlaggedQuestions() {
        return questionService.getFlaggedQuestions();
    }

    // UPDATE
    @PutMapping("/{questionId}")
    public QuestionResponse updateQuestion(@PathVariable Long questionId,
            @RequestBody QuestionRequest request) {
        return questionService.updateQuestion(questionId, request);
    }

    // DELETE
    @DeleteMapping("/{questionId}")
    public void deleteQuestion(@PathVariable Long questionId) {
        questionService.deleteQuestion(questionId);
    }

    // Hint için oy verme
    @PostMapping("/{questionId}/hints/{hintIndex}/vote")
    public ResponseEntity<String> voteHint(
            @PathVariable Long questionId,
            @PathVariable int hintIndex,
            @RequestParam boolean like) {
        questionService.voteHint(questionId, hintIndex, like);
        return ResponseEntity.ok("Vote recorded for hint");
    }

    // Explanation için oy verme
    @PostMapping("/{questionId}/explanation/vote")
    public ResponseEntity<String> voteExplanation(
            @PathVariable Long questionId,
            @RequestParam boolean like) {
        questionService.voteExplanation(questionId, like);
        return ResponseEntity.ok("Vote recorded for explanation");
    }
}
