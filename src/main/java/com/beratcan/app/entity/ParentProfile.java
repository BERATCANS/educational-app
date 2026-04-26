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
@Table(name = "parent_profiles")
public class ParentProfile {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false, unique = true)
        private User user;

        @Column(columnDefinition = "TIMESTAMP DEFAULT NOW()")
        private LocalDateTime createdAt = LocalDateTime.now();

        @ManyToMany(fetch = FetchType.LAZY)
        @JoinTable(name = "parent_student",
                joinColumns = @JoinColumn(name = "parent_profile_id"),
                inverseJoinColumns = @JoinColumn(name = "student_profile_id"))
        private Set<StudentProfile> children = new HashSet<>();
}
