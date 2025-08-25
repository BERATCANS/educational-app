package com.beratcan.app.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "options")
public class Option {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OptionLetter letter;

    public enum OptionType {
        TEXT,
        IMAGE
    }

    @Enumerated(EnumType.STRING)
    private OptionType type;

    private String content; // Text cevabı veya image URL

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
}
