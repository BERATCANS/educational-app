package com.beratcan.app.entity;

import com.beratcan.app.entity.enums.OptionLetter;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "question_results")
public class QuestionResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "test_result_id", nullable = false)
    @JsonBackReference
    private TestResult testResult;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_answer")
    private OptionLetter selectedAnswer; // Kullanıcının seçtiği şık

    @Column(nullable = false)
    private Boolean isCorrect = false; // Doğru mu yanlış mı

    @Column
    private Integer timeSpentSeconds; // Bu soruda geçirilen süre

    @Column(nullable = false)
    private LocalDateTime answeredAt = LocalDateTime.now();

    // Constructor
    public QuestionResult() {
    }

    public QuestionResult(TestResult testResult, Question question,
            OptionLetter selectedAnswer, Integer timeSpentSeconds) {
        this.testResult = testResult;
        this.question = question;
        this.selectedAnswer = selectedAnswer;
        this.timeSpentSeconds = timeSpentSeconds;
        this.answeredAt = LocalDateTime.now();
        this.isCorrect = question.getCorrectAnswer().equals(selectedAnswer);
    }
}
