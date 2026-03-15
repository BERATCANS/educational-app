package com.beratcan.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token boş olamaz")
    private String resetToken;

    @NotBlank(message = "Yeni şifre boş olamaz")
    @Size(min = 6, message = "Yeni şifre en az 6 karakter olmalıdır")
    @Size(max = 100, message = "Yeni şifre en fazla 100 karakter olabilir")
    private String newPassword;

    @NotBlank(message = "Şifre doğrulama boş olamaz")
    private String confirmPassword;
}
