package com.beratcan.app.service;

import com.beratcan.app.entity.User;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.UserNotFoundException;
import com.beratcan.app.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Email doğrulama işlemlerini yöneten servis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.email.from:noreply@yourapp.com}")
    private String fromEmail;

    /**
     * Email doğrulama token'ı oluştur ve gönder (asenkron)
     * Token kaydı transaction içinde, email gönderimi asenkron
     */
    @Transactional
    public void sendVerificationEmail(User user) {
        if (user.getEmailVerified()) {
            log.warn("Email already verified: userId={}, email={}", user.getId(), user.getEmail());
            return;
        }

        // Token oluştur ve kaydet (transaction içinde)
        String token = UUID.randomUUID().toString();
        user.setEmailVerificationToken(token);
        userRepository.save(user);

        // Email'i asenkron olarak gönder (ana işlemi bloklamaz)
        // ApplicationContext üzerinden çağır (self-invocation sorununu çözmek için)
        EmailVerificationService self = applicationContext.getBean(EmailVerificationService.class);
        self.sendVerificationEmailAsync(user, token);
    }

    /**
     * Email'i asenkron olarak gönder
     * Token zaten kaydedilmiş, sadece email gönderme işlemi asenkron
     */
    @Async
    public void sendVerificationEmailAsync(User user, String token) {
        String verificationUrl = baseUrl + "/api/users/verify-email?token=" + token;
        try {
            sendEmail(user.getEmail(), "Email Doğrulama - Hesabınızı Aktifleştirin",
                    buildVerificationEmail(user.getFirstName(), verificationUrl));
            log.info("Verification email sent (async): userId={}, email={}", user.getId(), user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email (async): userId={}, email={}",
                    user.getId(), user.getEmail(), e);
            // Email gönderilemese bile token kaydedildi, kullanıcı tekrar isteyebilir
        }
    }

    /**
     * Email doğrulama token'ını kontrol et ve doğrula
     */
    @Transactional
    public void verifyEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new BadRequestException("Doğrulama token'ı boş olamaz");
        }

        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Geçersiz doğrulama token'ı"));

        // Zaten doğrulanmış mı?
        if (user.getEmailVerified()) {
            log.info("Email already verified: userId={}, email={}", user.getId(), user.getEmail());
            throw new BadRequestException("Email zaten doğrulanmış.");
        }

        // Email'i doğrula
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setEmailVerificationToken(null);
        userRepository.save(user);

        log.info("Email verified successfully: userId={}, email={}", user.getId(), user.getEmail());
    }

    /**
     * Authenticated kullanıcı için doğrulama email'ini tekrar gönder
     * userId ile çağrılır (authentication'dan alınır)
     */
    @Transactional
    public void resendVerificationEmail(Long userId) {
        if (userId == null) {
            throw new BadRequestException("Kullanıcı ID'si gereklidir");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Kullanıcı bulunamadı: " + userId));

        if (user.getEmailVerified()) {
            throw new BadRequestException("Email zaten doğrulanmış.");
        }

        // Yeni token oluştur ve gönder
        sendVerificationEmail(user);
        log.info("Verification email resent: userId={}, email={}", user.getId(), user.getEmail());
    }

    /**
     * Email gönder
     */
    private void sendEmail(String to, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true); // HTML formatında
        helper.setFrom(fromEmail);

        mailSender.send(message);
        log.debug("Email sent successfully: to={}, subject={}", to, subject);
    }

    /**
     * Email doğrulama email template'i oluştur
     */
    private String buildVerificationEmail(String firstName, String verificationUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                        .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px; }
                        .button { display: inline-block; padding: 12px 30px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                        .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Email Doğrulama</h1>
                        </div>
                        <div class="content">
                            <p>Merhaba <strong>%s</strong>,</p>
                            <p>Hesabınızı aktifleştirmek için aşağıdaki butona tıklayın:</p>
                            <p style="text-align: center;">
                                <a href="%s" class="button">Email'i Doğrula</a>
                            </p>
                            <p>Veya aşağıdaki linki tarayıcınıza kopyalayın:</p>
                            <p style="word-break: break-all; color: #666;">%s</p>
                        </div>
                        <div class="footer">
                            <p>Bu email otomatik olarak gönderilmiştir. Lütfen yanıtlamayın.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(firstName != null ? firstName : "Kullanıcı", verificationUrl, verificationUrl);
    }
}
