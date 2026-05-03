package com.beratcan.app.service;

import com.beratcan.app.dto.request.ForgotPasswordRequest;
import com.beratcan.app.dto.request.ResetPasswordRequest;
import com.beratcan.app.dto.request.VerifyResetCodeRequest;
import com.beratcan.app.entity.User;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.repository.UserRepository;
import com.beratcan.app.constants.AppTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ApplicationContext applicationContext;

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 10;

    /**
     * 1. Adım: Şifre sıfırlama kodu gönder
     * Email bulunamasa bile 200 döner (enumeration attack önleme)
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail().trim().toLowerCase());

        if (userOpt.isEmpty()) {
            log.warn("Forgot password requested for non-existent email: {}", request.getEmail());
            return; // Sessizce devam et
        }

        User user = userOpt.get();

        String code = String.format("%06d", new Random().nextInt(1_000_000));
        user.setPasswordResetCode(code);
        user.setPasswordResetCodeExpiry(AppTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        // Önceki reset token varsa temizle
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        // Email'i asenkron gönder
        EmailVerificationService emailService = applicationContext.getBean(EmailVerificationService.class);
        emailService.sendPasswordResetEmail(user, code);

        log.info("Password reset code sent: userId={}", user.getId());
    }

    /**
     * 2. Adım: Kodu doğrula, reset token döndür
     */
    @Transactional
    public Map<String, String> verifyResetCode(VerifyResetCodeRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Geçersiz kod"));

        if (user.getPasswordResetCode() == null
                || !user.getPasswordResetCode().equals(request.getCode())) {
            throw new BadRequestException("Geçersiz kod");
        }

        if (AppTime.now().isAfter(user.getPasswordResetCodeExpiry())) {
            throw new BadRequestException("Kodun süresi dolmuş. Lütfen yeni kod isteyin.");
        }

        // Kodu temizle, reset token oluştur
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetCode(null);
        user.setPasswordResetCodeExpiry(null);
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(AppTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES));
        userRepository.save(user);

        log.info("Reset code verified, token issued: userId={}", user.getId());
        return Map.of("resetToken", resetToken);
    }

    /**
     * 3. Adım: Yeni şifreyi kaydet
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Şifreler eşleşmiyor");
        }

        User user = userRepository.findByPasswordResetToken(request.getResetToken())
                .orElseThrow(() -> new BadRequestException("Geçersiz veya süresi dolmuş token"));

        if (AppTime.now().isAfter(user.getPasswordResetTokenExpiry())) {
            throw new BadRequestException("Token süresi dolmuş. Lütfen tekrar şifre sıfırlama isteyin.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        // Mevcut oturumları geçersiz kıl
        user.setActiveSessionId(null);
        userRepository.save(user);

        log.info("Password reset successfully: userId={}", user.getId());
    }
}
