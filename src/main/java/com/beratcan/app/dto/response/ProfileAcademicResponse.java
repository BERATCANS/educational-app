package com.beratcan.app.dto.response;

import com.beratcan.app.entity.User;
import lombok.Data;

@Data
public class ProfileAcademicResponse {
    private String examType;
    private String gradeLevel;
    private String fieldType;
    private String schoolType;

    public static ProfileAcademicResponse fromUser(User user) {
        ProfileAcademicResponse response = new ProfileAcademicResponse();
        response.setExamType(user.getExamType() != null ? user.getExamType().name() : null);
        response.setGradeLevel(user.getGradeLevel() != null ? user.getGradeLevel().name() : null);
        response.setFieldType(user.getFieldType() != null ? user.getFieldType().name() : null);
        response.setSchoolType(user.getSchoolType() != null ? user.getSchoolType().name() : null);
        return response;
    }
}
