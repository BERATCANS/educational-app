package com.beratcan.app.entity;

import com.beratcan.app.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "student_profiles")
public class StudentProfile {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false, unique = true)
        private User user;

        @Enumerated(EnumType.STRING)
        private GradeLevel gradeLevel;

        @Enumerated(EnumType.STRING)
        private ExamType examType;

        @Enumerated(EnumType.STRING)
        private FieldType fieldType;

        @Enumerated(EnumType.STRING)
        private SchoolType schoolType;

        @Column(nullable = false, columnDefinition = "INT DEFAULT 200")
        private Integer dailyQuestionTarget = 200;

        @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
        private Integer dailySolvedQuestions = 0;

        @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
        private Integer currentStreak = 0;

        @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
        private Integer longestStreak = 0;

        @Column
        private LocalDate lastActivityDate;

        @Column(columnDefinition = "TIMESTAMP DEFAULT NOW()")
        private LocalDateTime createdAt = LocalDateTime.now();

        @ManyToMany
        @JoinTable(name = "student_flagged_questions",
                joinColumns = @JoinColumn(name = "student_profile_id"),
                inverseJoinColumns = @JoinColumn(name = "question_id"))
        private Set<Question> flaggedQuestions = new HashSet<>();

        @OneToMany(mappedBy = "studentProfile", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
        private List<TestResult> testResults = new ArrayList<>();

        @OneToOne(mappedBy = "studentProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        private Cart cart;

        @OneToMany(mappedBy = "studentProfile", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
        private List<Purchase> purchases = new ArrayList<>();

        @OneToMany(mappedBy = "studentProfile", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<BookReview> bookReviews = new ArrayList<>();

        @Column(name = "parent_invite_code", nullable = false, unique = true, length = 8)
        private String parentInviteCode;

        @ManyToMany(mappedBy = "children", fetch = FetchType.LAZY)
        private Set<ParentProfile> parents = new HashSet<>();

        @ManyToMany(mappedBy = "students", fetch = FetchType.LAZY)
        private Set<Classroom> classrooms = new HashSet<>();
}
