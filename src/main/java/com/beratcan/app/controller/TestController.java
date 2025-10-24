package com.beratcan.app.controller;

import com.beratcan.app.dto.request.TestRequest;
import com.beratcan.app.dto.response.TestResponse;
import com.beratcan.app.service.TestService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    // CREATE
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public TestResponse addTest(@RequestParam Long topicId, @RequestBody TestRequest request) {
        return testService.addTest(topicId, request);
    }

    // READ
    @GetMapping("/by-topic/{topicId}")
    public List<TestResponse> getTestsByTopic(@PathVariable Long topicId) {
        return testService.getTestsByTopic(topicId);
    }

    @GetMapping("/{testId}")
    public TestResponse getTestById(@PathVariable Long testId) {
        return testService.getTestById(testId);
    }

    // UPDATE
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update/{testId}")
    public TestResponse updateTest(@PathVariable Long testId, @RequestBody TestRequest request) {
        return testService.updateTest(testId, request);
    }

    // DELETE
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{testId}")
    public void deleteTest(@PathVariable Long testId) {
        testService.deleteTest(testId);
    }
}
