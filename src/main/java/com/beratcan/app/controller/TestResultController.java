package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.constants.AppConstants;
import com.beratcan.app.dto.request.TestSubmissionRequest;
import com.beratcan.app.dto.response.PageResponse;
import com.beratcan.app.dto.response.TestResultResponse;
import com.beratcan.app.entity.TestResult;
import com.beratcan.app.service.TestResultService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test-results")
public class TestResultController {

    private final TestResultService testResultService;

    public TestResultController(TestResultService testResultService) {
        this.testResultService = testResultService;
    }

    // Test sonucunu kaydet
    @PostMapping("/submit")
    @RateLimit(requestsPerMinute = 10, keyType = "USER", message = "Too many test submissions. Please try again later.")
    public ResponseEntity<TestResult> submitTest(
            @Valid @RequestBody TestSubmissionRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        TestResult result = testResultService.saveTestResult(
                userId, // Authentication'dan alınan userId
                request.getTestId(),
                request.getUserAnswers(),
                request.getTotalTimeSeconds());
        return ResponseEntity.ok(result);
    }

    // Kullanıcının kendi test sonuçları (pagination destekli)
    @GetMapping("/my-results")
    public ResponseEntity<PageResponse<TestResult>> getMyTestResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + AppConstants.DEFAULT_PAGE_SIZE) int size,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        PageResponse<TestResult> results = testResultService.getUserTestResults(userId, page, size);
        return ResponseEntity.ok(results);
    }

    // Belirli test sonucu detayı (sadece kendi sonucuna erişebilir)
    @GetMapping("/{testResultId}")
    public ResponseEntity<TestResult> getTestResult(
            @PathVariable Long testResultId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        TestResult result = testResultService.getTestResultById(testResultId, userId);
        return ResponseEntity.ok(result);
    }

    // Kullanıcının kendi istatistikleri
    @GetMapping("/my-stats")
    public ResponseEntity<Map<String, Object>> getMyStats(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Double avgSuccessRate = testResultService.getUserAverageSuccessRate(userId);
        Long totalTests = testResultService.getUserTotalTestCount(userId);

        Map<String, Object> stats = Map.of(
                "averageSuccessRate", avgSuccessRate != null ? avgSuccessRate : 0.0,
                "totalTestsCompleted", totalTests,
                "userId", userId);

        return ResponseEntity.ok(stats);
    }

    // Kullanıcının belirli bir test için kendi sonucu
    @GetMapping("/test/{testId}")
    public ResponseEntity<Map<String, Object>> getMyTestResult(
            @PathVariable Long testId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        TestResult result = testResultService.getUserTestResultByTestId(userId, testId);

        Map<String, Object> response = new HashMap<>();
        if (result != null) {
            response.put("found", true);
            response.put("data", result);
        } else {
            response.put("found", false);
            response.put("message", "No test result found for this test");
        }

        return ResponseEntity.ok(response);
    }

    // Kullanıcının belirli bir kitaptaki kendi test sonuçları
    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<TestResultResponse>> getMyTestResultsByBook(
            @PathVariable Long bookId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<TestResultResponse> results = testResultService.getUserTestResultsByBookId(userId, bookId);
        return ResponseEntity.ok(results);
    }
}
