package com.beratcan.app.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class ResendVerificationRequest {
    // Email veya userId ile çağrılabilir
    @Email(message = "Geçerli bir email adresi giriniz")
    private String email;

    private Long userId; // Email yerine userId ile de çağrılabilir
}
