package com.beratcan.app.controller;

import com.beratcan.app.dto.request.TestSubmissionRequest;
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
}
