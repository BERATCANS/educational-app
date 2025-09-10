package com.beratcan.app.dto.response;

import com.beratcan.app.entity.OptionLetter;
import lombok.Data;

import java.util.List;

@Data
public class QuestionResponse {
    private Long id;
    private String imageUrl;
    private int answerNumber ;
    private OptionLetter correctAnswer;
    private List<String> hints;
    private String explanation;
    private String videoUrl;

}
