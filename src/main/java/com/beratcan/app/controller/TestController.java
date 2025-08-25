package com.beratcan.app.controller;

import com.beratcan.app.entity.Test;
import com.beratcan.app.service.TestService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @PostMapping("/add")
    public Test addTest(@RequestParam Long topicId, @RequestParam String title) {
        return testService.addTest(topicId, title);
    }

    @GetMapping("/by-topic/{topicId}")
    public List<Test> getTestsByTopic(@PathVariable Long topicId) {
        return testService.getTestsByTopic(topicId);
    }
}
