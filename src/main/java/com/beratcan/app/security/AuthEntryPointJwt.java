package com.beratcan.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Authentication Entry Point
 * Authentication başarısız olduğunda veya token geçersiz olduğunda
 * standardize edilmiş JSON error response döner
 */
@Component
@Slf4j
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    private boolean isProduction() {
        return "prod".equals(activeProfile);
    }

    public AuthEntryPointJwt(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        // Response zaten commit edilmişse işlem yapma
        if (response.isCommitted()) {
            log.warn("Response already committed, cannot send error response for path: {}", request.getServletPath());
            return;
        }

        // DETAYLI LOG (her zaman - sadece server'da)
        log.error("Unauthorized error: path={}, error={}",
                request.getServletPath(),
                authException.getMessage(),
                authException);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // PRODUCTION'DA GENERIC MESAJ
        String message = isProduction()
                ? "Authentication required"
                : authException.getMessage();

        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", message);
        body.put("path", request.getServletPath());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
