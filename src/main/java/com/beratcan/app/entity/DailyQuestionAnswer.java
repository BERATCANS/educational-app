package com.beratcan.app.entity;

import com.beratcan.app.constants.AppTime;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.beratcan.app.entity.enums.OptionLetter;

@Entity
@Table(name = "daily_question_answers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"daily_question_id", "student_profile_id"}))
@Data
public class DailyQuestionAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_question_id", nullable = false)
    private DailyQuestion dailyQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OptionLetter selectedAnswer;

    @Column(nullable = false)
    private LocalDateTime answeredAt;

    @PrePersist
    protected void onCreate() {
        answeredAt = AppTime.now();
    }
}
