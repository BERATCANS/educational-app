package com.beratcan.app.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuestionAIConversationResponse {
    private Long conversationId;
    private Long questionId;
    private List<AIMessageResponse> messages;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
}
