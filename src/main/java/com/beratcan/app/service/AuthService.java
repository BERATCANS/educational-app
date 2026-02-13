package com.beratcan.app.service;

import com.beratcan.app.dto.request.LoginRequest;
import com.beratcan.app.dto.response.JwtResponse;
import com.beratcan.app.entity.User;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.UnauthorizedOperationException;
import com.beratcan.app.exception.UserNotFoundException;
import com.beratcan.app.repository.UserRepository;
import com.beratcan.app.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Authentication Service
 * Login, token refresh ve logout işlemlerini yönetir
 * Business logic controller'dan buraya taşındı
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    /**
     * Kullanıcı girişi yapar ve JWT token'ları oluşturur
     * Streak (günlük giriş serisi) bilgisini günceller
     * 
     * @param request Login request (identifier, password)
     * @return JwtResponse (accessToken, refreshToken, user info)
     */
    @Transactional
    public JwtResponse login(LoginRequest request) {
        // UserService ile kullanıcı kimlik bilgilerini doğrula
        User user = userService.validateUserCredentials(request);

        // Email doğrulama zorunlu değil - kullanıcı email doğrulamadan da login
        // yapabilir
        // Sadece bilgilendirme mesajı gönderilecek

        // Streak güncelleme artık ProfileService.getCurrentUser() içinde yapılıyor
        // (Her uygulama açılışında profile/me çağrıldığında otomatik güncellenir)

        // Yeni oturum ID'si oluştur ve kaydet (Concurrent Session Control)
        String sessionId = UUID.randomUUID().toString();
        user.setActiveSessionId(sessionId);
        userRepository.save(user); // Session ID'yi DB'ye kaydet

        // Token'ları oluştur (Session ID ile)
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getId(), sessionId);
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getId(), sessionId);

        // JwtResponse oluştur
        JwtResponse response = new JwtResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setType("Bearer");
        response.setExpiresIn(jwtUtil.getExpirationTimeInSeconds());
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());

        // Email doğrulanmamışsa bilgilendirme mesajı ekle
        if (!user.getEmailVerified()) {
            response.setEmailVerificationMessage(
                    "Email adresinizi doğrulamanız gerekiyor. Email doğrulama linki almak için uygulama içindeki 'Email Gönder' butonuna tıklayın.");
        }

        log.info("User logged in successfully: {} (ID: {})",
                user.getUsername(), user.getId());

        return response;
    }

    /**
     * Refresh token ile yeni access token ve refresh token oluşturur
     * Token rotation: Eski refresh token blacklist'e eklenir
     * 
     * @param refreshToken Refresh token
     * @return JwtResponse (yeni accessToken, yeni refreshToken, user info)
     */
    @Transactional(readOnly = true)
    public JwtResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new BadRequestException("Refresh token is required");
        }

        // 1. Refresh token geçerli mi kontrol et
        if (!jwtUtil.validateToken(refreshToken)) {
            log.warn("Invalid refresh token attempt");
            throw new UnauthorizedOperationException("Invalid or expired refresh token");
        }

        // 2. Refresh token mı kontrol et (Access token ile karışmasın)
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            log.warn("Attempted to use non-refresh token for refresh");
            throw new UnauthorizedOperationException("Not a refresh token");
        }

        // 3. Token'dan kullanıcı bilgilerini çıkar
        String username = jwtUtil.extractUsername(refreshToken);
        Long userId = jwtUtil.extractUserId(refreshToken);

        log.info("Refreshing token for user: {} (ID: {})", username, userId);

        // 4. User hala var mı kontrol et
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        // 5. YENİ tokenlar oluştur
        String newAccessToken = jwtUtil.generateAccessToken(username, userId, user.getActiveSessionId());
        String newRefreshToken = jwtUtil.generateRefreshToken(username, userId, user.getActiveSessionId());

        // 6. ESKİ refresh token'ı blacklist'e ekle (tek kullanımlık - token rotation)
        jwtUtil.blacklistToken(refreshToken);

        // 7. JwtResponse oluştur
        JwtResponse response = new JwtResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        response.setType("Bearer");
        response.setExpiresIn(jwtUtil.getExpirationTimeInSeconds());
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());

        log.info("Token refreshed successfully for user: {} (ID: {})", username, userId);
        return response;
    }

    /**
     * Logout işlemi - Token'ı blacklist'e ekler
     * 
     * @param token JWT token (access veya refresh token)
     */
    public void logout(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new BadRequestException("No token found in request");
        }

        // Token'ı blacklist'e ekle
        jwtUtil.blacklistToken(token);

        log.info("User logged out successfully. Token blacklisted.");
    }
}
