package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.request.CreateQuestionRequest;
import com.beratcan.app.dto.request.UpdateAnalysisRequest;
import com.beratcan.app.dto.request.UpdateQuestionRequest;
import com.beratcan.app.dto.response.AIAnalysisStatusResponse;
import com.beratcan.app.dto.response.QuestionResponse;
import com.beratcan.app.entity.Question;
import com.beratcan.app.entity.Test;
import com.beratcan.app.exception.QuestionNotFoundException;
import com.beratcan.app.exception.UnauthorizedOperationException;
import com.beratcan.app.repository.QuestionRepository;
import com.beratcan.app.repository.UserRepository;
import com.beratcan.app.service.QuestionService;
import com.beratcan.app.storage.FileServingHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
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
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final FileServingHelper fileServingHelper;

    // CREATE
    /**
     * Soru oluştur (görsel + temel bilgiler) + Otomatik AI analiz
     */
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
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

    /**
     * Question images are paid content — only owners (or admins) can fetch.
     * The storage path is resolved server-side from {@code Question.imageUrl};
     * the client only ever sends a question ID.
     */
    @GetMapping("/{questionId}/image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> getQuestionImage(
            @PathVariable Long questionId,
            Authentication auth) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority())
                        || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));

        if (!isAdmin) {
            Long userId = Long.parseLong(auth.getName());
            Test parentTest = q.getTest();
            Long bookId = parentTest.getTopic().getBook().getId();
            if (!userRepository.existsByIdAndOwnedBooks_Id(userId, bookId)) {
                throw new UnauthorizedOperationException("Bu kitaba erişiminiz yok");
            }
        }

        return fileServingHelper.serve(
                q.getImageUrl(),
                FileServingHelper.mediaTypeFor(q.getImageUrl()),
                FileServingHelper.CacheVisibility.PRIVATE);
    }

    /**
     * Soru için AI analiz durumunu getir
     */
    @GetMapping("/{questionId}/ai-status")
    @RateLimit(requestsPerMinute = 60, keyType = "USER", message = "Too many status requests. Please try again later.")
    public AIAnalysisStatusResponse getAIAnalysisStatus(@PathVariable Long questionId) {
        return questionService.getAIAnalysisStatus(questionId);
    }

    // UPDATE
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{questionId}")
    public QuestionResponse updateQuestion(@PathVariable Long questionId,
            @Valid @ModelAttribute UpdateQuestionRequest request) {
        return questionService.updateQuestion(questionId, request);
    }

    // DELETE
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateAnalysis(
            @PathVariable Long questionId,
            @RequestBody UpdateAnalysisRequest request) {

        questionService.updateAnalysis(questionId, request.getHints(), request.getExplanation());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Analysis updated successfully"));
    }
}
