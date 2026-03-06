package com.beratcan.app.entity;

import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.entity.enums.LessonType;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "books", indexes = {
        @Index(name = "idx_book_exam_type", columnList = "exam_type"),
        @Index(name = "idx_book_grade_level", columnList = "grade_level"),
        @Index(name = "idx_book_paid", columnList = "paid"),
        @Index(name = "idx_book_price", columnList = "price")
})
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    @Column(columnDefinition = "TEXT")
    private String description; // Kitap açıklaması (max 2000 karakter)

    @Enumerated(EnumType.STRING)
    private GradeLevel gradeLevel; // Kitap hangi sınıf için?

    @Enumerated(EnumType.STRING)
    private ExamType examType; // Kitap hangi sınav için?

    @Enumerated(EnumType.STRING)
    private LessonType lessonType; // Kitap yazarı

    @Column(length = 500)
    private String coverImageUrl; // Kitap kapak fotoğrafı URL'si

    @Column(nullable = false)
    private boolean paid = false; // Kitap paralı mı?

    @Column(precision = 10, scale = 2)
    private BigDecimal price; // Kitap fiyatı (TL)

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookCode> codes = new ArrayList<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Topic> topics = new ArrayList<>(); // Kitap konuları

    @ManyToMany(mappedBy = "ownedBooks")
    private Set<User> owners = new HashSet<>(); // Kitabı sahiplenen kullanıcılar

    // ✅ CartItem ile bidirectional ilişki (Sepetlerdeki bu kitap)
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CartItem> cartItems = new ArrayList<>();

    // ✅ PurchaseItem ile bidirectional ilişki (Bu kitabın satın alma geçmişi)
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PurchaseItem> purchaseItems = new ArrayList<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookReview> reviews = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer numberOfTopics = 0;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer numberOfTests = 0;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer numberOfQuestions = 0;

    @Column(nullable = false, columnDefinition = "REAL DEFAULT 0.0")
    private Double averageOverallRating = 0.0;

    @Column(nullable = false, columnDefinition = "REAL DEFAULT 0.0")
    private Double averageContentQualityRating = 0.0;

    @Column(nullable = false, columnDefinition = "REAL DEFAULT 0.0")
    private Double averageDifficultyRating = 0.0;

    @Column(nullable = false, columnDefinition = "REAL DEFAULT 0.0")
    private Double averageVideoQualityRating = 0.0;

    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long reviewCount = 0L; // Toplam yorum sayısı
}
