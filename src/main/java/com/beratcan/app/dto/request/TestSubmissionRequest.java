package com.beratcan.app.dto.request;

import com.beratcan.app.entity.enums.OptionLetter;
import lombok.Data;

import java.util.Map;

@Data
public class TestSubmissionRequest {
    private Long userId;
    private Long testId;
    private Map<Long, OptionLetter> userAnswers; // questionId -> selectedAnswer
    private Integer totalTimeSeconds;

    // Örnek JSON:
    // {
    // "userId": 1,
    // "testId": 1,
    // "userAnswers": {
    // "2": "A",
    // "3": "B",
    // "4": "C"
    // },
    // "totalTimeSeconds": 300
    // }
}
