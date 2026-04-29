package com.beratcan.app.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateClassroomRequest {

    @Size(max = 100, message = "Sınıf adı en fazla 100 karakter olabilir")
    private String name;

    @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
    private String description;

    private String gradeLevel;

    private String examType;
}
