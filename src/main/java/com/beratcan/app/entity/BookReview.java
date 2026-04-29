package com.beratcan.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_reviews", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "book_id", "student_profile_id" })
}, indexes = {
        @Index(name = "idx_book_review_book_id", columnList = "book_id"),
        @Index(name = "idx_book_review_student_profile", columnList = "student_profile_id")
})
@Getter
@Setter
public class BookReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @Column(nullable = false)
    private int overallRating;

    @Column(nullable = false)
    private int contentQualityRating;

    @Column(nullable = false)
    private int difficultyRating;

    @Column(nullable = false)
    private int videoQualityRating;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
