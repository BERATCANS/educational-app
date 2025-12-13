package com.beratcan.app.dto.response;

import com.beratcan.app.entity.enums.OptionLetter;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionResultResponse {
    private Long id;
    private Long questionId; // Question entity yerine sadece ID (lazy loading sorunu yok)
    private OptionLetter selectedAnswer;
    private Boolean isCorrect;
    private Integer timeSpentSeconds;
    private LocalDateTime answeredAt;
}
