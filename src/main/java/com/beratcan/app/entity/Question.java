package com.beratcan.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "questions")
//email verify ekle
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private int answerNumber = 5;

    @Enumerated(EnumType.STRING)
    private OptionLetter correctAnswer;

    @Column(nullable = false)
    private int sequenceNumber;
}
