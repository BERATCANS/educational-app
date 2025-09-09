package com.beratcan.app.dto.response;

import com.beratcan.app.entity.OptionLetter;
import lombok.Data;

@Data
public class QuestionResponse {
    private Long id;
    private String imageUrl;
    private int answerNumber ;
    private OptionLetter correctAnswer;
}
