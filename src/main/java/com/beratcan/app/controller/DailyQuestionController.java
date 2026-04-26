package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.request.DailyQuestionAnswerRequest;
import com.beratcan.app.dto.request.CreateDailyQuestionRequest;
import com.beratcan.app.dto.request.UpdateDailyQuestionRequest;
import com.beratcan.app.dto.response.DailyQuestionResponse;
import com.beratcan.app.entity.DailyQuestion;
import com.beratcan.app.service.DailyQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/daily-questions")
@RequiredArgsConstructor
public class DailyQuestionController {

    private final DailyQuestionService dailyQuestionService;

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
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

    // ADMIN ENDPOINTS

    /**
     * Tekil günlük soru getir (Admin) - Zaman kontrolü yapmadan tüm bilgileri
     * gösterir
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/{questionId}")
    public ResponseEntity<DailyQuestionResponse> getDailyQuestionById(@PathVariable Long questionId) {
        DailyQuestionResponse response = dailyQuestionService.getDailyQuestionByIdForAdmin(questionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Tüm günlük soruları getir (Admin)
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<DailyQuestionResponse>> getAllDailyQuestions() {
        List<DailyQuestionResponse> questions = dailyQuestionService.getAllDailyQuestions();
        return ResponseEntity.ok(questions);
    }

    /**
     * Günlük soruyu güncelle (Admin)
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{questionId}")
    public ResponseEntity<DailyQuestionResponse> updateDailyQuestion(
            @PathVariable Long questionId,
            @Valid @ModelAttribute UpdateDailyQuestionRequest request) {
        DailyQuestionResponse response = dailyQuestionService.updateDailyQuestion(questionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Günlük soruyu sil (Admin)
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteDailyQuestion(@PathVariable Long questionId) {
        dailyQuestionService.deleteDailyQuestion(questionId);
        return ResponseEntity.noContent().build();
    }
}