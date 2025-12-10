package com.beratcan.app.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AIMessageResponse {
    private Long id;
    private String content;
    private String role; // USER veya ASSISTANT
    private LocalDateTime createdAt;
}
