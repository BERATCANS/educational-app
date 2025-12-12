package com.beratcan.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBookCodeRequest {
    @NotBlank(message = "Kod boş olamaz")
    @Size(max = 100, message = "Kod en fazla 100 karakter olabilir")
    private String code;
}
