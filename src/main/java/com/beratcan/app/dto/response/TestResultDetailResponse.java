package com.beratcan.app.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TestResultDetailResponse {
    private Long id;
    private Integer score;
    private Integer totalQuestions;
    private Double successRate;
    private Integer timeSpentSeconds;
    private LocalDateTime completedAt;

    // Test bilgileri (iç içe DTO yerine direkt field'lar)
    private Long testId;
    private String testTitle;
    private Integer testNumberOfQuestions;
    private Integer testSequenceNumber;

    private List<QuestionResultResponse> questionResults; // Kullanıcının soru cevapları
}
