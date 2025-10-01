package com.beratcan.app.dto.request;

import com.beratcan.app.entity.enums.OptionLetter;
import lombok.Data;

@Data
public class QuestionRequest {
    private String imageUrl;
    private OptionLetter correctAnswer;
    private int answerNumber;
    private String videoUrl;
    private Integer sequenceNumber; // Opsiyonel, belirtilmezse otomatik atanır
}
