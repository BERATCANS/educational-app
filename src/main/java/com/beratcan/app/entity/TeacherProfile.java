package com.beratcan.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "teacher_profiles")
public class TeacherProfile {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false, unique = true)
        private User user;

        @Column(columnDefinition = "TIMESTAMP DEFAULT NOW()")
        private LocalDateTime createdAt = LocalDateTime.now();

        @ManyToMany(mappedBy = "teachers", fetch = FetchType.LAZY)
        private Set<Classroom> classrooms = new HashSet<>();
}
