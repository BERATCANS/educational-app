package com.beratcan.app.controller;

import com.beratcan.app.dto.request.TestSubmissionRequest;
import com.beratcan.app.dto.response.TestResultResponse;
import com.beratcan.app.entity.TestResult;
import com.beratcan.app.service.TestResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<TestResult> submitTest(@RequestBody TestSubmissionRequest request) {
        TestResult result = testResultService.saveTestResult(
                request.getUserId(),
                request.getTestId(),
                request.getUserAnswers(),
                request.getTotalTimeSeconds());
        return ResponseEntity.ok(result);
    }

    // Kullanıcının test sonuçları
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TestResult>> getUserTestResults(@PathVariable Long userId) {
        List<TestResult> results = testResultService.getUserTestResults(userId);
        return ResponseEntity.ok(results);
    }

    // Belirli test sonucu detayı
    @GetMapping("/{testResultId}")
    public ResponseEntity<TestResult> getTestResult(@PathVariable Long testResultId) {
        TestResult result = testResultService.getTestResultById(testResultId);
        return ResponseEntity.ok(result);
    }

    // Kullanıcı istatistikleri
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable Long userId) {
        Double avgSuccessRate = testResultService.getUserAverageSuccessRate(userId);
        Long totalTests = testResultService.getUserTotalTestCount(userId);

        Map<String, Object> stats = Map.of(
                "averageSuccessRate", avgSuccessRate != null ? avgSuccessRate : 0.0,
                "totalTestsCompleted", totalTests,
                "userId", userId);

        return ResponseEntity.ok(stats);
    }

    // Kullanıcının belirli bir test için sonucu
    @GetMapping("/user/{userId}/test/{testId}")
    public ResponseEntity<TestResult> getUserTestResult(@PathVariable Long userId, @PathVariable Long testId) {
        TestResult result = testResultService.getUserTestResultByTestId(userId, testId);
        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Kullanıcının belirli bir kitaptaki tüm test sonuçları
    @GetMapping("/user/{userId}/book/{bookId}")
    public ResponseEntity<List<TestResultResponse>> getUserTestResultsByBookId(@PathVariable Long userId,
            @PathVariable Long bookId) {
        try {
            List<TestResultResponse> results = testResultService.getUserTestResultsByBookId(userId, bookId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}
