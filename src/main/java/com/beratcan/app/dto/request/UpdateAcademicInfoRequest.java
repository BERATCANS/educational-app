package com.beratcan.app.dto.request;

import lombok.Data;

@Data
public class UpdateAcademicInfoRequest {
    private String examType;
    private String gradeLevel;
    private String fieldType;
    private String schoolType;
}
