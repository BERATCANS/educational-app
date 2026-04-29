package com.beratcan.app.dto.response;

import com.beratcan.app.entity.StudentProfile;
import lombok.Data;

@Data
public class ProfileAcademicResponse {
    private String examType;
    private String gradeLevel;
    private String fieldType;
    private String schoolType;

    public static ProfileAcademicResponse fromStudentProfile(StudentProfile profile) {
        ProfileAcademicResponse response = new ProfileAcademicResponse();
        if (profile != null) {
            response.setExamType(profile.getExamType() != null ? profile.getExamType().name() : null);
            response.setGradeLevel(profile.getGradeLevel() != null ? profile.getGradeLevel().name() : null);
            response.setFieldType(profile.getFieldType() != null ? profile.getFieldType().name() : null);
            response.setSchoolType(profile.getSchoolType() != null ? profile.getSchoolType().name() : null);
        }
        return response;
    }
}
