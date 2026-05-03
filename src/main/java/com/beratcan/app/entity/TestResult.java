package com.beratcan.app.entity;

import com.beratcan.app.constants.AppTime;
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
@Table(name = "test_results", indexes = {
        @Index(name = "idx_test_results_student_profile", columnList = "student_profile_id"),
        @Index(name = "idx_test_result_test_id", columnList = "test_id"),
        @Index(name = "idx_test_result_completed_at", columnList = "completed_at")
})
public class TestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_profile_id", nullable = false)
    @JsonBackReference
    private StudentProfile studentProfile;

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
    private LocalDateTime completedAt = AppTime.now();

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
