package com.beratcan.app.entity;

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
    @AttributeOverrides({
            @AttributeOverride(name = "text", column = @Column(name = "explanation_text", columnDefinition = "TEXT")),
            @AttributeOverride(name = "likes", column = @Column(name = "explanation_likes")),
            @AttributeOverride(name = "dislikes", column = @Column(name = "explanation_dislikes"))
    })
    private Explanation explanation;

    @Column(length = 500)
    private String videoUrl;

    @Column(name = "sequence_number")
    private Integer sequenceNumber;

}