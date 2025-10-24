package com.beratcan.app.controller;

import com.beratcan.app.dto.response.JwtResponse;
import com.beratcan.app.dto.response.UserResponse;
import com.beratcan.app.dto.response.FlaggedResponse;
import com.beratcan.app.dto.request.LoginRequest;
import com.beratcan.app.dto.request.RefreshTokenRequest;
import com.beratcan.app.dto.request.UserRequest;
import com.beratcan.app.entity.Question;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.City;
import com.beratcan.app.repository.UserRepository;
import com.beratcan.app.service.UserService;
import com.beratcan.app.security.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRequest userRequest) {
        try {
            User user = userService.registerUser(userRequest);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
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
        } catch (IllegalArgumentException e) {
            log.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Logout endpoint
     * Token'ı blacklist'e ekler, böylece bir daha kullanılamaz
     * POST /api/users/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);

            if (token != null && !token.isEmpty()) {
                // Token'ı blacklist'e ekle
                jwtUtil.blacklistToken(token);
                return ResponseEntity.ok(Map.of(
                        "message", "Logout successful",
                        "status", "success"));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "No token found in request",
                        "status", "error"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Logout failed: " + e.getMessage(),
                    "status", "error"));
        }
    }

    /**
     * Token Refresh Endpoint
     * Expired access token'ı yenilemek için kullanılır
     * POST /api/users/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();

            // 1. Refresh token boş mu kontrol et
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Refresh token is required"));
            }

            // 2. Refresh token geçerli mi kontrol et
            if (!jwtUtil.validateToken(refreshToken)) {
                log.warn("Invalid refresh token attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired refresh token"));
            }

            // 3. Refresh token mı kontrol et (Access token ile karışmasın)
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                log.warn("Attempted to use non-refresh token for refresh");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not a refresh token"));
            }

            // 4. Token'dan kullanıcı bilgilerini çıkar
            String username = jwtUtil.extractUsername(refreshToken);
            Long userId = jwtUtil.extractUserId(refreshToken);

            log.info("Refreshing token for user: {} (ID: {})", username, userId);

            // 5. User hala var mı kontrol et
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            // 6. YENİ tokenlar oluştur
            String newAccessToken = jwtUtil.generateAccessToken(username, userId);
            String newRefreshToken = jwtUtil.generateRefreshToken(username, userId);

            // 7. ESKİ refresh token'ı blacklist'e ekle (tek kullanımlık - rotation)
            jwtUtil.blacklistToken(refreshToken);

            // 8. Response hazırla
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

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token refresh failed", "details", e.getMessage()));
        }
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

    @PostMapping("/{userId}/flag/{questionId}")
    public ResponseEntity<?> flagQuestion(@PathVariable Long userId, @PathVariable Long questionId) {
        try {
            userService.flagQuestion(userId, questionId);
            return ResponseEntity.ok(Map.of("message", "Question flagged successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Flag kaldırma
    @DeleteMapping("/{userId}/flag/{questionId}")
    public ResponseEntity<?> unflagQuestion(@PathVariable Long userId, @PathVariable Long questionId) {
        try {
            userService.unflagQuestion(userId, questionId);
            return ResponseEntity.ok(Map.of("message", "Question unflagged successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Kullanıcının çözemediği sorular
    @GetMapping("/{userId}/flagged")
    public ResponseEntity<List<FlaggedResponse>> getFlaggedQuestions(@PathVariable Long userId) {
        Set<Question> flaggedQuestions = userService.getFlaggedQuestions(userId);
        List<FlaggedResponse> response = flaggedQuestions.stream()
                .map(question -> {
                    FlaggedResponse fr = new FlaggedResponse();
                    fr.setQuestionId(question.getId());
                    fr.setImageUrl(question.getImageUrl());
                    fr.setAnswerNumber(question.getAnswerNumber());
                    fr.setCorrectAnswer(question.getCorrectAnswer().name());
                    fr.setVideoUrl(question.getVideoUrl());

                    // Hints ve explanation
                    fr.setHints(question.getHints() != null
                            ? question.getHints().stream().map(hint -> hint.getText()).toList()
                            : List.of());
                    fr.setExplanation(question.getExplanation() != null ? question.getExplanation().getText() : "");

                    // Navigation ve display bilgileri
                    if (question.getTest() != null) {
                        fr.setTestId(question.getTest().getId());
                        fr.setTestTitle(question.getTest().getTitle());

                        if (question.getTest().getTopic() != null) {
                            fr.setTopicId(question.getTest().getTopic().getId());
                            fr.setTopicTitle(question.getTest().getTopic().getTitle());

                            if (question.getTest().getTopic().getBook() != null) {
                                fr.setBookId(question.getTest().getTopic().getBook().getId());
                                fr.setBookTitle(question.getTest().getTopic().getBook().getTitle());
                            }
                        }
                    }

                    return fr;
                })
                .toList();
        return ResponseEntity.ok(response);
    }

    // Tek soru için flagged bilgileri - navigation için book, topic, test ID'leri
    // dahil
    @GetMapping("/flagged/{questionId}")
    public ResponseEntity<FlaggedResponse> getFlaggedQuestionById(@PathVariable Long questionId) {
        try {
            // Question'ı direkt repository'den al
            Question question = userService.getQuestionById(questionId);

            if (question == null) {
                return ResponseEntity.notFound().build();
            }

            FlaggedResponse fr = new FlaggedResponse();
            fr.setQuestionId(question.getId());
            fr.setImageUrl(question.getImageUrl());
            fr.setAnswerNumber(question.getAnswerNumber());
            fr.setCorrectAnswer(question.getCorrectAnswer().name());
            fr.setVideoUrl(question.getVideoUrl());

            fr.setHints(question.getHints() != null ? question.getHints().stream().map(hint -> hint.getText()).toList()
                    : List.of());
            fr.setExplanation(question.getExplanation() != null ? question.getExplanation().getText() : "");

            if (question.getTest() != null) {
                fr.setTestId(question.getTest().getId());
                fr.setTestTitle(question.getTest().getTitle());

                if (question.getTest().getTopic() != null) {
                    fr.setTopicId(question.getTest().getTopic().getId());
                    fr.setTopicTitle(question.getTest().getTopic().getTitle());

                    if (question.getTest().getTopic().getBook() != null) {
                        fr.setBookId(question.getTest().getTopic().getBook().getId());
                        fr.setBookTitle(question.getTest().getTopic().getBook().getTitle());
                    }
                }
            }

            return ResponseEntity.ok(fr);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
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
