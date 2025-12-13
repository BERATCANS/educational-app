package com.beratcan.app.entity;

import com.beratcan.app.entity.enums.MessageRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "question_ai_messages")
public class QuestionAIMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "conversation_id", nullable = false)
    private QuestionAIConversation conversation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private Integer tokenCount; // AI response için token sayısı (maliyet takibi)

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
