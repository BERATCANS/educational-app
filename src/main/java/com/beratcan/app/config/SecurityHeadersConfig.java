package com.beratcan.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityHeadersConfig implements WebMvcConfigurer {

    @Bean
    public org.springframework.security.web.header.writers.StaticHeadersWriter staticHeadersWriter() {
        return new org.springframework.security.web.header.writers.StaticHeadersWriter(
                "X-Content-Type-Options", "nosniff",
                "X-Frame-Options", "DENY",
                "X-XSS-Protection", "1; mode=block",
                "Cache-Control", "no-cache, no-store, must-revalidate",
                "Pragma", "no-cache",
                "Expires", "0",
                "Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload",
                "Referrer-Policy", "strict-origin-when-cross-origin",
                "Permissions-Policy", "geolocation=(), microphone=(), camera=()");
    }
}
