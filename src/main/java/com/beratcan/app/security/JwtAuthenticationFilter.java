package com.beratcan.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter
 * Her request'te JWT token'ı kontrol eder
 * Token geçerliyse kullanıcıyı SecurityContext'e ekler
 * Role-based authorization için authorities (roles) bilgisini de ekler
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String header = request.getHeader("Authorization");

            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);

                // Token blacklist'te mi kontrol et (validateToken içinde de var ama açıkça
                // kontrol edelim)
                if (jwtUtil.isTokenBlacklisted(token)) {
                    log.warn("Attempted to use blacklisted token from IP: {}", request.getRemoteAddr());
                    sendUnauthorizedResponse(response, "Token has been invalidated (logged out)");
                    return;
                }

                // Token geçerli mi kontrol et
                if (jwtUtil.validateToken(token)) {
                    String username = jwtUtil.extractUsername(token);
                    Long userId = jwtUtil.extractUserId(token);

                    log.debug("Authenticated request for user: {} (ID: {})", username, userId);

                    // UserDetailsService ile kullanıcıyı yükle (role bilgisi ile birlikte)
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // Authorities (role'ler) ile authentication oluştur
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId.toString(), // Principal (userId)
                            null, // Credentials (şifre gerekmez)
                            userDetails.getAuthorities() // ✅ Authorities (role'ler) eklendi!
                    );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // SecurityContext'e kullanıcıyı ekle
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("User authenticated with roles: {}", userDetails.getAuthorities());
                } else {
                    log.warn("Invalid token from IP: {}", request.getRemoteAddr());
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Unauthorized response gönder
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(String.format(
                "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"%s\"}",
                message));
    }
}
