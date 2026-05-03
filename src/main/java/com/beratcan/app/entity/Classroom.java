package com.beratcan.app.entity;

import com.beratcan.app.constants.AppTime;
import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.GradeLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "classrooms")
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_teacher_profile_id")
    private TeacherProfile creator;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_level", length = 50)
    private GradeLevel gradeLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", length = 50)
    private ExamType examType;

    @Column(name = "student_join_code", nullable = false, unique = true, length = 8)
    private String studentJoinCode;

    @Column(name = "teacher_join_code", nullable = false, unique = true, length = 8)
    private String teacherJoinCode;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = AppTime.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "classroom_teacher",
            joinColumns = @JoinColumn(name = "classroom_id"),
            inverseJoinColumns = @JoinColumn(name = "teacher_profile_id"))
    private Set<TeacherProfile> teachers = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "classroom_student",
            joinColumns = @JoinColumn(name = "classroom_id"),
            inverseJoinColumns = @JoinColumn(name = "student_profile_id"))
    private Set<StudentProfile> students = new HashSet<>();
}
