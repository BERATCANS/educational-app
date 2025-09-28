package com.beratcan.app.dto.response;

import lombok.Data;

@Data
public class TestResultResponse {
    private Long userId;
    private Long testId;
    private Double successRate;
}
