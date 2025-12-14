package com.beratcan.app.service;

import com.beratcan.app.dto.request.FeedbackRequest;
import com.beratcan.app.entity.User;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.UserNotFoundException;
import com.beratcan.app.repository.UserRepository;
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

import jakarta.mail.MessagingException;

/**
 * Feedback servisi - Kullanıcı geri bildirimlerini yönetir
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${app.feedback.email:info@kitappai.com}")
    private String feedbackEmail;

    @Value("${app.email.from:noreply@yourapp.com}")
    private String fromEmail;

    /**
     * Feedback gönder - Kullanıcı bilgileri ile birlikte mail gönderir
     */
    @Transactional
    public void sendFeedback(Long userId, FeedbackRequest request) {
        if (request == null) {
            throw new BadRequestException("Feedback request boş olamaz");
        }

        // Kullanıcıyı getir
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        log.info("Feedback alındı: userId={}, title={}", userId, request.getTitle());

        // Mail'i asenkron olarak gönder
        // ApplicationContext üzerinden çağır (self-invocation sorununu çözmek için)
        FeedbackService self = applicationContext.getBean(FeedbackService.class);
        self.sendFeedbackEmailAsync(user, request);
    }

    /**
     * Feedback email'ini asenkron olarak gönder
     */
    @Async
    public void sendFeedbackEmailAsync(User user, FeedbackRequest request) {
        try {
            String subject = "Kullanıcı Geri Bildirimi: " + request.getTitle();
            String body = buildFeedbackEmailBody(user, request);

            sendEmail(feedbackEmail, subject, body);
            log.info("Feedback email gönderildi: userId={}, to={}", user.getId(), feedbackEmail);
        } catch (Exception e) {
            log.error("Feedback email gönderme hatası: userId={}, error={}",
                    user.getId(), e.getMessage(), e);
            // Email gönderilemese bile feedback kaydedilmiş olabilir (gelecekte DB'ye
            // kaydedilirse)
        }
    }

    /**
     * Feedback email içeriğini oluştur
     */
    private String buildFeedbackEmailBody(User user, FeedbackRequest request) {
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
                        .info-section { background-color: white; padding: 15px; margin: 15px 0; border-radius: 5px; border-left: 4px solid #4CAF50; }
                        .info-label { font-weight: bold; color: #555; }
                        .info-value { color: #333; margin-left: 10px; }
                        .feedback-section { background-color: white; padding: 15px; margin: 15px 0; border-radius: 5px; }
                        .feedback-title { font-size: 18px; font-weight: bold; color: #333; margin-bottom: 10px; }
                        .feedback-text { color: #555; white-space: pre-wrap; }
                        .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Kullanıcı Geri Bildirimi</h1>
                        </div>
                        <div class="content">
                            <div class="info-section">
                                <div style="margin-bottom: 10px;">
                                    <span class="info-label">Kullanıcı Adı:</span>
                                    <span class="info-value">%s</span>
                                </div>
                                <div style="margin-bottom: 10px;">
                                    <span class="info-label">E-posta:</span>
                                    <span class="info-value">%s</span>
                                </div>
                                <div>
                                    <span class="info-label">Telefon:</span>
                                    <span class="info-value">%s</span>
                                </div>
                            </div>

                            <div class="feedback-section">
                                <div class="feedback-title">%s</div>
                                <div class="feedback-text">%s</div>
                            </div>
                        </div>
                        <div class="footer">
                            <p>Bu email otomatik olarak gönderilmiştir.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(
                        user.getUsername() != null ? user.getUsername() : "Bilinmiyor",
                        user.getEmail() != null ? user.getEmail() : "Bilinmiyor",
                        user.getPhoneNumber() != null ? user.getPhoneNumber() : "Bilinmiyor",
                        escapeHtml(request.getTitle()),
                        escapeHtml(request.getText()));
    }

    /**
     * HTML escape için basit metod
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br>");
    }

    /**
     * Email gönder
     */
    private void sendEmail(String to, String subject, String body) throws MessagingException {
        jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true); // HTML formatında
        helper.setFrom(fromEmail);

        mailSender.send(message);
        log.debug("Email sent successfully: to={}, subject={}", to, subject);
    }
}
