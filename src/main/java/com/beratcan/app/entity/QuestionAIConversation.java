package com.beratcan.app.entity;

import com.beratcan.app.constants.AppTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "question_ai_conversations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_question_conversation", columnNames = { "user_id", "question_id" })
})
public class QuestionAIConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionAIMessage> messages = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastMessageAt;

    @PrePersist
    protected void onCreate() {
        createdAt = AppTime.now();
    }
}
