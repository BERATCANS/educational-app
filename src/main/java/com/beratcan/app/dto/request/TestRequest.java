package com.beratcan.app.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class TestRequest {
    private String title;
    private List<QuestionRequest> questions; // Opsiyonel, soru eklemek için
}
