package com.beratcan.app.dto.request;

import com.beratcan.app.entity.enums.OptionLetter;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateDailyQuestionRequest {
    private MultipartFile image;
    private Integer answerNumber;
    private OptionLetter correctAnswer;
    private String videoUrl;
    private String explanation;
    private LocalDate displayDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
