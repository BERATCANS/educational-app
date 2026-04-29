package com.beratcan.app.dto.response;

import com.beratcan.app.entity.StudentProfile;
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
    private Boolean streakUpdated;
    private Integer currentStreak;
    private Integer longestStreak;
    // Veli davet kodu — sadece STUDENT için doludur (StudentProfile.parentInviteCode).
    // Diğer rollerde null döner.
    private String parentInviteCode;

    public UserResponse(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.username = user.getUsername();
        this.role = user.getRole().name();
        this.gender = user.getGender() != null ? user.getGender().name() : null;
        this.city = user.getCity() != null ? user.getCity().name() : null;
        this.streakUpdated = false;
        this.currentStreak = 0;
        this.longestStreak = 0;

        // StudentProfile varsa öğrenci alanlarını doldur
        StudentProfile sp = user.getStudentProfile();
        if (sp != null) {
            this.gradeLevel = sp.getGradeLevel() != null ? sp.getGradeLevel().name() : null;
            this.examType = sp.getExamType() != null ? sp.getExamType().name() : null;
            this.fieldType = sp.getFieldType() != null ? sp.getFieldType().name() : null;
            this.schoolType = sp.getSchoolType() != null ? sp.getSchoolType().name() : null;
            this.currentStreak = sp.getCurrentStreak() != null ? sp.getCurrentStreak() : 0;
            this.longestStreak = sp.getLongestStreak() != null ? sp.getLongestStreak() : 0;
            this.parentInviteCode = sp.getParentInviteCode();
        }
    }

    public UserResponse(User user, Boolean streakUpdated) {
        this(user);
        this.streakUpdated = streakUpdated;
    }
}