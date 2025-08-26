package com.beratcan.app.controller;

import com.beratcan.app.dto.QuestionRequest;
import com.beratcan.app.entity.Question;
import com.beratcan.app.service.QuestionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping("/add")
    public Question addQuestion(@RequestBody QuestionRequest request) {
        return questionService.addQuestion(request);
    }

    @GetMapping("/by-test/{testId}")
    public List<Question> getQuestionsByTest(@PathVariable Long testId) {
        return questionService.getQuestionsByTest(testId);
    }
}
