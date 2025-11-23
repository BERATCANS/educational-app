package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.response.JwtResponse;
import com.beratcan.app.dto.response.UserResponse;
import com.beratcan.app.dto.request.LoginRequest;
import com.beratcan.app.dto.request.RefreshTokenRequest;
import com.beratcan.app.dto.request.UserRequest;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.City;
import com.beratcan.app.service.AuthService;
import com.beratcan.app.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    @RateLimit(requestsPerMinute = 3, keyType = "IP", message = "Too many registration attempts. Please try again later.")
    public ResponseEntity<User> register(@RequestBody UserRequest userRequest) {
        User user = userService.registerUser(userRequest);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    @RateLimit(requestsPerMinute = 5, keyType = "IP", message = "Too many login attempts. Please try again later.")
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest request) {
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
     * POST /api/users/refresh
     */
    @PostMapping("/refresh")
    @RateLimit(requestsPerMinute = 10, keyType = "IP", message = "Too many token refresh attempts. Please try again later.")
    public ResponseEntity<JwtResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().trim().isEmpty()) {
            throw new com.beratcan.app.exception.BadRequestException("Refresh token is required");
        }

        // AuthService'e delegate et - tüm business logic orada
        JwtResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Request'ten JWT token'ı çıkar
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    // Kullanıcı profile bilgileri
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(new UserResponse(user));
    }

    @GetMapping("/cities")
    public City[] getCities() {
        return City.values();
    }
}
