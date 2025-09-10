package com.beratcan.app.dto.request;

import com.beratcan.app.entity.OptionLetter;
import lombok.Data;

import java.util.List;

@Data
public class QuestionRequest {
    private String imageUrl;
    private OptionLetter correctAnswer;
    private int answerNumber;
    private String videoUrl;
}
