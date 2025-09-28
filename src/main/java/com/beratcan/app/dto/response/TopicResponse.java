package com.beratcan.app.dto.response;

import lombok.Data;

@Data
public class TopicResponse {
    private Long id;
    private String title;
    private Integer numberOfTests;
    private Integer numberOfQuestions;
}
