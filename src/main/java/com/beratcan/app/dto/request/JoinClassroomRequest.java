package com.beratcan.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JoinClassroomRequest {

    @NotBlank(message = "Sınıf kodu boş olamaz")
    @Size(min = 6, max = 8, message = "Sınıf kodu 6-8 karakter olmalıdır")
    private String studentJoinCode;
}
