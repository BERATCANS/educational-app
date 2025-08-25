package com.beratcan.app.dto;

import com.beratcan.app.entity.QuestionPart;
import lombok.Data;

@Data
public class QuestionPartRequest {
    private QuestionPart.PartType type; // TEXT veya IMAGE
    private String content;             // Text içeriği veya image URL

    public QuestionPart.PartType getType() {
        return type != null ? type : QuestionPart.PartType.TEXT;
    }
}

