package com.beratcan.app.dto.request;

import lombok.Data;

/**
 * Refresh Token Request DTO
 * Access token yenilemek için kullanılır
 */
@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
