package com.beratcan.app.controller;

import com.beratcan.app.dto.request.DailyQuestionAnswerRequest;
import com.beratcan.app.dto.request.CreateDailyQuestionRequest;
import com.beratcan.app.dto.response.DailyQuestionResponse;
import com.beratcan.app.entity.DailyQuestion;
import com.beratcan.app.entity.enums.OptionLetter;
import java.time.LocalDate;
import java.time.LocalTime;
import com.beratcan.app.service.DailyQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/daily-questions")
@RequiredArgsConstructor
public class DailyQuestionController {

    private final DailyQuestionService dailyQuestionService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<?> createDailyQuestion(
            @RequestParam("image") MultipartFile image,
            @RequestParam("answerNumber") Integer answerNumber,
            @RequestParam("correctAnswer") String correctAnswer,
            @RequestParam(value = "videoUrl", required = false) String videoUrl,
            @RequestParam("explanation") String explanation,
            @RequestParam("displayDate") String displayDate,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime) {
        CreateDailyQuestionRequest request = new CreateDailyQuestionRequest();
        request.setImage(image);
        request.setAnswerNumber(answerNumber);
        request.setCorrectAnswer(OptionLetter.valueOf(correctAnswer));
        request.setVideoUrl(videoUrl);
        request.setExplanation(explanation);
        request.setDisplayDate(LocalDate.parse(displayDate));
        request.setStartTime(LocalTime.parse(startTime));
        request.setEndTime(LocalTime.parse(endTime));

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
    public ResponseEntity<DailyQuestionResponse> submitAnswer(
            @RequestBody DailyQuestionAnswerRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        DailyQuestionResponse response = dailyQuestionService.submitAnswer(userId, request);
        return ResponseEntity.ok(response);
    }
}