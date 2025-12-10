package com.beratcan.app.entity;

import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.OptionLetter;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "test_id", nullable = false)
    @JsonBackReference
    private Test test;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private int answerNumber = 5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OptionLetter correctAnswer;

    @ElementCollection
    @CollectionTable(name = "question_hints", joinColumns = @JoinColumn(name = "question_id"))
    private List<Hint> hints = new ArrayList<>();

    @Embedded
    private Explanation explanation;

    @Column(length = 500)
    private String videoUrl;

    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type")
    private ExamType examType; // Denormalized: Test → Topic → Book → ExamType

}
