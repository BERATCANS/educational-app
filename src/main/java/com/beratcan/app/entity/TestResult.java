package com.beratcan.app.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "test_results")
public class TestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @ManyToOne
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(nullable = false)
    private Integer score = 0; // Doğru cevap sayısı

    @Column(nullable = false)
    private Integer totalQuestions = 0; // Toplam soru sayısı

    @Column(nullable = false)
    private Double successRate = 0.0; // Başarı oranı (%)

    @Column
    private Integer timeSpentSeconds; // Toplam geçen süre

    @Column(nullable = false)
    private LocalDateTime completedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "testResult", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuestionResult> questionResults = new ArrayList<>();

    // Başarı oranını hesapla
    public void calculateSuccessRate() {
        if (totalQuestions > 0) {
            this.successRate = (score * 100.0) / totalQuestions;
        } else {
            this.successRate = 0.0;
        }
    }
}
