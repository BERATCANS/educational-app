package com.beratcan.app.dto.response;

import lombok.Data;

@Data
public class TestResponse {
    private Long id;
    private String title;
    private Integer numberOfQuestions;
    private Integer sequenceNumber;
}
