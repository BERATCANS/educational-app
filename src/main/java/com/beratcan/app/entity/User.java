package com.beratcan.app.entity;

import com.beratcan.app.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
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

        @Column(nullable = false)
        private String firstName;

        @Column(nullable = false)
        private String lastName;

        @Column(nullable = false, unique = true)
        private String email;

        @Column(nullable = false, unique = true)
        private String phoneNumber;

        @Column(nullable = false, unique = true)
        private String username; // unique username

        @Column(nullable = false)
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

        @ManyToMany
        @JoinTable(name = "user_books", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "book_id"), uniqueConstraints = @UniqueConstraint(name = "uk_user_book", columnNames = {
                        "user_id", "book_id" }))
        private Set<Book> ownedBooks = new HashSet<>();

        @ManyToMany
        @JoinTable(name = "user_flagged_questions", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "question_id"))
        private Set<Question> flaggedQuestions = new HashSet<>();

        @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private List<TestResult> testResults = new ArrayList<>();

        // ✅ Cart ile bidirectional ilişki (Her kullanıcının bir sepeti var)
        @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        private Cart cart;

        // ✅ Purchase ile bidirectional ilişki (Kullanıcının satın alma geçmişi)
        @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private List<Purchase> purchases = new ArrayList<>();

        @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<BookReview> bookReviews = new ArrayList<>();
}
