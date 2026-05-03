package com.beratcan.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LinkChildRequest {

    @NotBlank(message = "Davet kodu boş olamaz")
    @Size(min = 6, max = 8, message = "Davet kodu 6-8 karakter olmalıdır")
    private String code;
}
