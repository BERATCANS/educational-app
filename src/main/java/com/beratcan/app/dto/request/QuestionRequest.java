package com.beratcan.app.dto.request;

import com.beratcan.app.entity.OptionLetter;
import lombok.Data;

@Data
public class QuestionRequest {
    private Long testId;
    private String imageUrl;
    private OptionLetter correctAnswer;
    private int sequenceNumber;
    private int answerNumber; // opsiyonel, default 5
}


