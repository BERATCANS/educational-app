package com.beratcan.app.entity;

import com.beratcan.app.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;

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
        private Role role; // STUDENT / TEACHER / ADMIN / PARENT / SUPER_ADMIN

        @Enumerated(EnumType.STRING)
        private Gender gender; // KADIN / ERKEK / DİĞER

        @Enumerated(EnumType.STRING)
        private City city; // 81 il + DİĞER

        // Email doğrulama alanları
        @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
        private Boolean emailVerified = false;

        @Column(length = 100)
        private String emailVerificationToken;

        @Column
        private java.time.LocalDateTime emailVerifiedAt;

        @Column
        private String activeSessionId; // Tekil oturum ID'si (UUID) - Concurrent session kontrolü için

        // Şifre sıfırlama alanları
        @Column(length = 6)
        private String passwordResetCode; // 6 haneli OTP

        @Column
        private java.time.LocalDateTime passwordResetCodeExpiry;

        @Column(length = 36)
        private String passwordResetToken;

        @Column
        private java.time.LocalDateTime passwordResetTokenExpiry;

        // Profil ilişkileri (OneToOne, mappedBy)
        @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        private StudentProfile studentProfile;

        @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        private TeacherProfile teacherProfile;

        @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        private AdminProfile adminProfile;

        @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        private ParentProfile parentProfile;

        @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        private SuperAdminProfile superAdminProfile;

        @ManyToMany
        @JoinTable(name = "user_books",
                        joinColumns = @JoinColumn(name = "user_id"),
                        inverseJoinColumns = @JoinColumn(name = "book_id"),
                        uniqueConstraints = @UniqueConstraint(name = "uk_user_book", columnNames = {
                                        "user_id", "book_id" }))
        private Set<Book> ownedBooks = new HashSet<>();

        @Column(nullable = false)
        private boolean isDeleted = false; // Soft Delete mantığı için
}
