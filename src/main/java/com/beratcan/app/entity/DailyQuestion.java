package com.beratcan.app.entity;

import com.beratcan.app.constants.AppTime;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import com.beratcan.app.entity.enums.OptionLetter;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "daily_questions")
@Data
@SQLDelete(sql = "UPDATE daily_questions SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class DailyQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Integer answerNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OptionLetter correctAnswer;

    @Column(length = 500)
    private String videoUrl;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false)
    private LocalDate displayDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private boolean active = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = AppTime.now();
    }

    @Column(nullable = false)
    private boolean isDeleted = false; // Soft Delete mantığı için
}
