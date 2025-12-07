package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kayıt işlemi response DTO
 * Email doğrulanmamışsa email bilgisi döner
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private Long userId;
    private String email;
    private Boolean emailVerified;
    private String message;
}
