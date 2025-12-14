package com.beratcan.app.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AIMessageResponse {
    private Long id;
    private String content; // Her zaman text (voice mesajlar da text'e çevrilmiş hali)
    private String role; // USER veya ASSISTANT
    private String messageType; // TEXT, VOICE_INPUT, ASSISTANT_TEXT, ASSISTANT_VOICE
    private String audioUrl; // Geçici audio URL (sadece ASSISTANT_VOICE için, nullable)
    private LocalDateTime createdAt;
}
