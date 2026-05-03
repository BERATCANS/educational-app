package com.beratcan.app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Security validation configuration
 * Validates security settings on application startup
 */
@Slf4j
@Component
public class SecurityValidationConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Validate JWT secret on application startup.
     * Placeholder boşsa Spring placeholder çözülemez ve startup zaten patlar;
     * burada yalnızca uzunluk kontrolü yapılır (prod'da hard fail, dev'de uyarı).
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void validateJwtSecret() {
        if ("prod".equals(activeProfile)) {
            if (jwtSecret.length() < 32) {
                log.error("❌ JWT_SECRET is too short ({} characters). Minimum 32 characters required in production!",
                        jwtSecret.length());
                throw new IllegalStateException(
                        String.format(
                                "JWT_SECRET must be at least 32 characters long in production. Current length: %d",
                                jwtSecret.length()));
            }
            log.info("✅ JWT_SECRET validation passed (length: {} characters)", jwtSecret.length());
        } else if (jwtSecret.length() < 32) {
            log.warn("⚠️  JWT_SECRET is too short ({} characters). Minimum 32 characters recommended.",
                    jwtSecret.length());
        } else {
            log.info("✅ JWT_SECRET validation passed (length: {} characters)", jwtSecret.length());
        }
    }
}
