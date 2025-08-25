package com.beratcan.app.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "question_parts")
public class QuestionPart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public enum PartType {
        TEXT,
        IMAGE
    }

    @Enumerated(EnumType.STRING)
    private PartType type; // TEXT ya da IMAGE

    private String content; // text içeriği veya image URL

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
}
