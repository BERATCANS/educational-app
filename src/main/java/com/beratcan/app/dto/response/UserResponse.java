package com.beratcan.app.dto.response;

import com.beratcan.app.entity.User;
import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String username;
    private String role;
    private String gradeLevel;
    private String examType;
    private String fieldType;
    private String schoolType;
    private String gender;
    private String city;
    private Boolean streakUpdated; // Streak güncellenip güncellenmediği bilgisi
    private Integer currentStreak; // Mevcut günlük giriş serisi (kaç gün)
    private Integer longestStreak; // En uzun günlük giriş serisi

    public UserResponse(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.username = user.getUsername();
        this.role = user.getRole().name();
        this.gradeLevel = user.getGradeLevel() != null ? user.getGradeLevel().name() : null;
        this.examType = user.getExamType() != null ? user.getExamType().name() : null;
        this.fieldType = user.getFieldType() != null ? user.getFieldType().name() : null;
        this.schoolType = user.getSchoolType() != null ? user.getSchoolType().name() : null;
        this.gender = user.getGender() != null ? user.getGender().name() : null;
        this.city = user.getCity() != null ? user.getCity().name() : null;
        this.streakUpdated = false; // Default false, setter ile set edilecek
        this.currentStreak = user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;
        this.longestStreak = user.getLongestStreak() != null ? user.getLongestStreak() : 0;
    }

    public UserResponse(User user, Boolean streakUpdated) {
        this(user);
        this.streakUpdated = streakUpdated;
    }
}