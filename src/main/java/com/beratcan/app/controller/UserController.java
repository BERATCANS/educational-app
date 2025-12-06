package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.response.JwtResponse;
import com.beratcan.app.dto.request.LoginRequest;
import com.beratcan.app.dto.request.UserRequest;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.City;
import com.beratcan.app.service.AuthService;
import com.beratcan.app.service.EmailVerificationService;
import com.beratcan.app.service.UserService;
import com.beratcan.app.dto.response.RegisterResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    @RateLimit(requestsPerMinute = 3, keyType = "IP", message = "Too many registration attempts. Please try again later.")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody UserRequest userRequest) {
        User user = userService.registerUser(userRequest);

        // Email doğrulanmamışsa özel response döner
        com.beratcan.app.dto.response.RegisterResponse response = new com.beratcan.app.dto.response.RegisterResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setEmailVerified(user.getEmailVerified());
        response.setMessage(
                "Kayıt başarılı! Email doğrulama linki email adresinize gönderildi. Lütfen email kutunuzu kontrol edin ve doğrulama linkine tıklayın. Email gelmediyse uygulama içindeki 'Email Tekrar Gönder' butonuna tıklayın.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @RateLimit(requestsPerMinute = 5, keyType = "IP", message = "Too many login attempts. Please try again later.")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        // AuthService'e delegate et - tüm business logic orada
        JwtResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout endpoint
     * Token'ı blacklist'e ekler, böylece bir daha kullanılamaz
     * POST /api/users/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);

        // AuthService'e delegate et - tüm business logic orada
        authService.logout(token);

        return ResponseEntity.ok(Map.of(
                "message", "Logout successful",
                "status", "success"));
    }

    /**
     * Token Refresh Endpoint
     * Expired access token'ı yenilemek için kullanılır
     * Authenticated endpoint: Refresh token Authorization header'da olmalı
     * POST /api/users/refresh
     */
    @PostMapping("/refresh")
    @RateLimit(requestsPerMinute = 10, keyType = "USER", message = "Too many token refresh attempts. Please try later.")
    public ResponseEntity<JwtResponse> refreshToken(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        // Refresh token'ı Authorization header'dan al
        String refreshToken = extractTokenFromRequest(httpRequest);

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new com.beratcan.app.exception.BadRequestException(
                    "Refresh token is required in Authorization header");
        }

        // AuthService'e delegate et - tüm business logic orada
        JwtResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    @GetMapping("/cities")
    @Cacheable("cities")
    public City[] getCities() {
        return City.values();
    }

    /**
     * Email doğrulama endpoint'i
     * GET /api/users/verify-email?token=xxx
     */
    @GetMapping("/verify-email")
    @RateLimit(requestsPerMinute = 3, keyType = "IP", message = "Too many verification attempts. Please try again later.")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(Map.of(
                "message", "Email başarıyla doğrulandı",
                "status", "success"));
    }

}
