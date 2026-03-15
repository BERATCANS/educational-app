package com.beratcan.app.entity;

import com.beratcan.app.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Setter
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Table(name = "users", uniqueConstraints = {
                @UniqueConstraint(columnNames = { "email" }),
                @UniqueConstraint(columnNames = { "phoneNumber" }),
                @UniqueConstraint(columnNames = { "username" })
}, indexes = {
                @Index(name = "idx_user_email", columnList = "email"),
                @Index(name = "idx_user_username", columnList = "username")
})
public class User {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, length = 50)
        private String firstName;

        @Column(nullable = false, length = 50)
        private String lastName;

        @Column(nullable = false, unique = true, length = 100)
        private String email;

        @Column(nullable = false, unique = true, length = 20)
        private String phoneNumber; // Format: +?[0-9]{10,13} (max 14 karakter: + işareti + 13 rakam)

        @Column(nullable = false, unique = true, length = 30)
        private String username; // unique username

        @Column(nullable = false, length = 100)
        private String password; // hashlenmiş

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private Role role; // STUDENT / TEACHER / ADMIN

        @Enumerated(EnumType.STRING)
        private GradeLevel gradeLevel; // 1-12 veya MEZUN

        @Enumerated(EnumType.STRING)
        private ExamType examType; // LGS / YKS / KPSS / DİĞER

        @Enumerated(EnumType.STRING)
        private FieldType fieldType; // sadece YKS için zorunlu olabilir (nullable bırakabilirsin)

        @Enumerated(EnumType.STRING)
        private Gender gender; // KADIN / ERKEK / DİĞER

        @Enumerated(EnumType.STRING)
        private City city; // 81 il + DİĞER

        @Enumerated(EnumType.STRING)
        private SchoolType schoolType; // sadece YKS için zorunlu olabilir (nullable bırakabilirsin)

        @Column(nullable = false, columnDefinition = "INT DEFAULT 200")
        private Integer dailyQuestionTarget = 200;

        @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
        private Integer dailySolvedQuestions = 0; // Bugün çözülen soru sayısı (Her gece yarısı otomatik sıfırlanır)

        @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
        private Integer currentStreak = 0; // Mevcut günlük giriş serisi

        @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
        private Integer longestStreak = 0; // En uzun günlük giriş serisi

        @Column
        private LocalDate lastActivityDate; // Son aktivite tarihi (nullable - uygulama açma, soru çözme vb.) - Streak
                                            // takibi için kullanılır

        // Email doğrulama alanları
        @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
        private Boolean emailVerified = false; // Email doğrulandı mı?

        @Column(length = 100)
        private String emailVerificationToken; // Email doğrulama token'ı

        @Column
        private java.time.LocalDateTime emailVerifiedAt; // Email ne zaman doğrulandı

        @Column
        private String activeSessionId; // Tekil oturum ID'si (UUID) - Concurrent session kontrolü için

        // Şifre sıfırlama alanları
        @Column(length = 6)
        private String passwordResetCode; // 6 haneli OTP

        @Column
        private java.time.LocalDateTime passwordResetCodeExpiry; // OTP'nin son geçerlilik tarihi (5 dk)

        @Column(length = 36)
        private String passwordResetToken; // OTP doğrulandıktan sonra verilen UUID token

        @Column
        private java.time.LocalDateTime passwordResetTokenExpiry; // Reset token'ın son geçerlilik tarihi (10 dk)

        @ManyToMany
        @JoinTable(name = "user_books", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "book_id"), uniqueConstraints = @UniqueConstraint(name = "uk_user_book", columnNames = {
                        "user_id", "book_id" }))
        private Set<Book> ownedBooks = new HashSet<>();

        @ManyToMany
        @JoinTable(name = "user_flagged_questions", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "question_id"))
        private Set<Question> flaggedQuestions = new HashSet<>();

        @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
        private List<TestResult> testResults = new ArrayList<>();

        // ✅ Cart ile bidirectional ilişki (Her kullanıcının bir sepeti var)
        @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        private Cart cart;

        // ✅ Purchase ile bidirectional ilişki (Kullanıcının satın alma geçmişi)
        @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
        private List<Purchase> purchases = new ArrayList<>();

        @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<BookReview> bookReviews = new ArrayList<>();

        @Column(nullable = false)
        private boolean isDeleted = false; // Soft Delete mantığı için
}
