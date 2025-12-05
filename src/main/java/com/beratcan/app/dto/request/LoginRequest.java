package com.beratcan.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email veya kullanıcı adı boş olamaz")
    private String identifier; // email veya username

    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır")
    @Size(max = 100, message = "Şifre en fazla 100 karakter olabilir")
    private String password;
}
