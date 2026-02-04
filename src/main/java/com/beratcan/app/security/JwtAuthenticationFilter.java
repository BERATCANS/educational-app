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
import org.springframework.security.web.context.SecurityContextRepository;
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
    private final SecurityContextRepository securityContextRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService,
            SecurityContextRepository securityContextRepository) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String header = request.getHeader("Authorization");

            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);

                // Token blacklist'te mi kontrol et
                if (jwtUtil.isTokenBlacklisted(token)) {
                    log.warn("Attempted to use blacklisted token from IP: {}", request.getRemoteAddr());
                    sendUnauthorizedResponse(response, "Token has been invalidated (logged out)");
                    return;
                }

                // Önce access token olarak kontrol et
                boolean isAccessToken = jwtUtil.isAccessToken(token);
                boolean isRefreshToken = jwtUtil.isRefreshToken(token);

                authenticateUser(request, response, token, isAccessToken, isRefreshToken);
            } else {
                // Header yoksa query param'a bak (SSE / EventSource için gerekli)
                String token = request.getParameter("token");
                if (token == null) {
                    token = request.getParameter("access_token");
                }

                if (token != null && !token.trim().isEmpty()) {
                    boolean isAccessToken = jwtUtil.isAccessToken(token);
                    // Query param ile gelen refresh token kabul edilmez (güvenlik)
                    if (isAccessToken && jwtUtil.validateToken(token)) {
                        authenticateUser(request, response, token, true, false);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage(), e);
            // Exception oluştu ama response commit edilmediyse filter chain'i devam ettir
            // Spring Security'nin exception handler'ı devreye girecek
        }

        // Response commit edilmediyse filter chain'i devam ettir
        if (!response.isCommitted()) {
            filterChain.doFilter(request, response);
        }
    }

    private void authenticateUser(HttpServletRequest request, HttpServletResponse response, String token, boolean isAccessToken, boolean isRefreshToken) {
        // Access token geçerli mi kontrol et
        if (isAccessToken && jwtUtil.validateToken(token)) {
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
            // Async dispatch (SSE tamamlanınca) için request attribute'a kaydet
            securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);

            log.debug("User authenticated with roles: {}", userDetails.getAuthorities());
        }
        // Access token expire olmuşsa ama refresh token geçerliyse, authenticate et
        // (Sadece /api/users/refresh endpoint'i için)
        else if (isRefreshToken && jwtUtil.validateToken(token) &&
                request.getRequestURI().equals("/api/users/refresh")) {
            String username = jwtUtil.extractUsername(token);
            Long userId = jwtUtil.extractUserId(token);

            log.debug("Authenticated refresh request for user: {} (ID: {})", username, userId);

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
            securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);

            log.debug("User authenticated with refresh token for refresh endpoint");
        } else {
            log.warn("Invalid or expired token from IP: {}", request.getRemoteAddr());
        }
    }

    /**
     * Unauthorized response gönder
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        // Response zaten commit edilmişse işlem yapma
        if (response.isCommitted()) {
            log.warn("Response already committed, cannot send unauthorized response");
            return;
        }
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(String.format(
                "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"%s\"}",
                message));
    }
}
