package com.beratcan.app.controller;

import com.beratcan.app.dto.OptionRequest;
import com.beratcan.app.dto.QuestionPartRequest;
import com.beratcan.app.entity.OptionLetter;
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
    public Question addQuestion(@RequestParam Long testId,
                                @RequestBody List<QuestionPartRequest> parts,
                                @RequestBody List<OptionRequest> options,
                                @RequestParam OptionLetter correctAnswer,
                                @RequestParam int pageNumber) {
        return questionService.addQuestion(testId, parts, options, correctAnswer, pageNumber);
    }

    @GetMapping("/by-test/{testId}")
    public List<Question> getQuestionsByTest(@PathVariable Long testId) {
        return questionService.getQuestionsByTest(testId);
    }
}
