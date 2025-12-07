package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT authentication response
 * Login ve refresh başarılı olduğunda dönen response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String accessToken; // JWT access token (kısa ömürlü)
    private String refreshToken; // JWT refresh token (uzun ömürlü)
    private String type = "Bearer"; // Token tipi
    private Long expiresIn; // Access token expire süresi (saniye)
    private Long userId; // Kullanıcı ID
    private String username; // Kullanıcı adı
    private String email; // Email
    private String role; // Kullanıcı rolü (STUDENT, TEACHER, ADMIN)

    // Email doğrulanmamışsa bilgilendirme mesajı
    private String emailVerificationMessage; // null: email doğrulanmış, string: doğrulama mesajı

    // Backward compatibility için eski constructor
    @Deprecated
    public JwtResponse(String token, String username, String role) {
        this.accessToken = token;
        this.type = "Bearer";
        this.username = username;
        this.role = role;
    }

    // Backward compatibility için token getter/setter
    @Deprecated
    public String getToken() {
        return accessToken;
    }

    @Deprecated
    public void setToken(String token) {
        this.accessToken = token;
    }
}
