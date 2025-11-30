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

import java.time.LocalDate;

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

        // Email doğrulanmamışsa giriş yapamaz
        if (!user.getEmailVerified()) {
            log.warn("Login blocked - email not verified: userId={}, email={}",
                    user.getId(), user.getEmail());
            throw new BadRequestException(
                    "Email adresiniz doğrulanmamış. Lütfen email kutunuzu kontrol edin ve doğrulama linkine tıklayın. "
                            +
                            "Email gelmediyse 'Tekrar Email Gönder' butonuna tıklayarak yeni bir doğrulama email'i alabilirsiniz.");
        }

        // Streak güncelleme mantığı - o gün ilk giriş mi kontrol et
        boolean streakUpdated = updateStreak(user);

        // Token'ları oluştur
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getId());

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

        // Streak güncellendi mi? Frontend bu flag'i görürse /api/profile/streak
        // endpoint'ini çağırabilir
        response.setStreakUpdated(streakUpdated);

        if (streakUpdated) {
            log.info("User logged in successfully: {} (ID: {}), Streak updated: {}",
                    user.getUsername(), user.getId(), user.getCurrentStreak());
        } else {
            log.info("User logged in successfully: {} (ID: {}), Streak unchanged: {}",
                    user.getUsername(), user.getId(), user.getCurrentStreak());
        }

        return response;
    }

    /**
     * Kullanıcının streak (günlük giriş serisi) bilgisini günceller
     * - İlk giriş: streak = 1
     * - Bugün zaten giriş yapılmışsa: streak değişmez
     * - Dün giriş yapılmışsa: streak += 1
     * - Dünden önce giriş yapılmışsa: streak = 1 (reset)
     * 
     * @param user Kullanıcı entity
     * @return true eğer streak güncellendiyse (o gün ilk giriş), false eğer
     *         değişmediyse
     */
    private boolean updateStreak(User user) {
        LocalDate today = LocalDate.now();
        LocalDate lastLogin = user.getLastLoginDate();

        // İlk giriş veya lastLogin null ise
        if (lastLogin == null) {
            user.setCurrentStreak(1);
            user.setLastLoginDate(today);
            if (user.getLongestStreak() < 1) {
                user.setLongestStreak(1);
            }
            userRepository.save(user);
            log.debug("First login for user {}: streak set to 1", user.getUsername());
            return true; // Streak güncellendi
        }

        // Bugün zaten giriş yapılmışsa streak değişmez
        if (lastLogin.equals(today)) {
            log.debug("User {} already logged in today, streak unchanged: {}",
                    user.getUsername(), user.getCurrentStreak());
            return false; // Streak değişmedi
        }

        // Dün giriş yapılmışsa streak artır
        LocalDate yesterday = today.minusDays(1);
        if (lastLogin.equals(yesterday)) {
            int newStreak = user.getCurrentStreak() + 1;
            user.setCurrentStreak(newStreak);
            user.setLastLoginDate(today);

            // En uzun streak'i güncelle
            if (newStreak > user.getLongestStreak()) {
                user.setLongestStreak(newStreak);
            }

            userRepository.save(user);
            log.info("User {} streak increased: {} -> {}",
                    user.getUsername(), user.getCurrentStreak() - 1, newStreak);
            return true; // Streak güncellendi
        }

        // Dünden önce giriş yapılmışsa streak reset
        user.setCurrentStreak(1);
        user.setLastLoginDate(today);
        if (user.getLongestStreak() < 1) {
            user.setLongestStreak(1);
        }
        userRepository.save(user);
        log.info("User {} streak reset to 1 (last login: {})",
                user.getUsername(), lastLogin);
        return true; // Streak güncellendi (reset)
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
        String newAccessToken = jwtUtil.generateAccessToken(username, userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(username, userId);

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
