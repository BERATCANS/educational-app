package com.beratcan.app.dto.request;

import com.beratcan.app.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token boş olamaz")
    private String resetToken;

    @NotBlank(message = "Yeni şifre boş olamaz")
    @Size(max = 100, message = "Yeni şifre en fazla 100 karakter olabilir")
    @ValidPassword
    private String newPassword;

    @NotBlank(message = "Şifre doğrulama boş olamaz")
    private String confirmPassword;
}
