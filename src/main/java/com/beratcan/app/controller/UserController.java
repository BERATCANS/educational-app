package com.beratcan.app.controller;

import com.beratcan.app.dto.response.JwtResponse;
import com.beratcan.app.dto.response.UserResponse;
import com.beratcan.app.dto.request.LoginRequest;
import com.beratcan.app.dto.request.RefreshTokenRequest;
import com.beratcan.app.dto.request.UserRequest;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.City;
import com.beratcan.app.repository.UserRepository;
import com.beratcan.app.service.UserService;
import com.beratcan.app.security.JwtUtil;

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
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody UserRequest userRequest) {
        User user = userService.registerUser(userRequest);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest request) {
        User user = userService.login(request, jwtUtil);

        // ✅ Hem Access hem Refresh token oluştur
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getId());

        // JwtResponse tüm bilgileri içeriyor
        JwtResponse response = new JwtResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setType("Bearer");
        response.setExpiresIn(jwtUtil.getExpirationTimeInSeconds());
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());

        log.info("User logged in successfully: {}", user.getUsername());
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

        if (token == null || token.isEmpty()) {
            throw new com.beratcan.app.exception.BadRequestException("No token found in request");
        }

        // Token'ı blacklist'e ekle
        jwtUtil.blacklistToken(token);
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
    public ResponseEntity<JwtResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().trim().isEmpty()) {
            throw new com.beratcan.app.exception.BadRequestException("Refresh token is required");
        }

        String refreshToken = request.getRefreshToken();

        // 1. Refresh token geçerli mi kontrol et
        if (!jwtUtil.validateToken(refreshToken)) {
            log.warn("Invalid refresh token attempt");
            throw new com.beratcan.app.exception.UnauthorizedOperationException("Invalid or expired refresh token");
        }

        // 2. Refresh token mı kontrol et (Access token ile karışmasın)
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            log.warn("Attempted to use non-refresh token for refresh");
            throw new com.beratcan.app.exception.UnauthorizedOperationException("Not a refresh token");
        }

        // 3. Token'dan kullanıcı bilgilerini çıkar
        String username = jwtUtil.extractUsername(refreshToken);
        Long userId = jwtUtil.extractUserId(refreshToken);

        log.info("Refreshing token for user: {} (ID: {})", username, userId);

        // 4. User hala var mı kontrol et
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.beratcan.app.exception.UserNotFoundException("User not found: " + username));

        // 5. YENİ tokenlar oluştur
        String newAccessToken = jwtUtil.generateAccessToken(username, userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(username, userId);

        // 6. ESKİ refresh token'ı blacklist'e ekle (tek kullanımlık - rotation)
        jwtUtil.blacklistToken(refreshToken);

        // 7. Response hazırla
        JwtResponse response = new JwtResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        response.setType("Bearer");
        response.setExpiresIn(jwtUtil.getExpirationTimeInSeconds());
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());

        log.info("Token refreshed successfully for user: {}", username);
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
