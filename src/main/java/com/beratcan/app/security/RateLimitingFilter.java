package com.beratcan.app.security;

import com.beratcan.app.service.RateLimitingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;

    public RateLimitingFilter(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIpAddress(request);
        String requestPath = request.getRequestURI();

        // Check rate limits based on endpoint
        boolean isAllowed = switch (requestPath) {
            case "/api/users/login" -> rateLimitingService.isLoginAllowed(clientIp);
            case "/api/users/register" -> rateLimitingService.isRegistrationAllowed(clientIp);
            default -> rateLimitingService.isApiCallAllowed(clientIp);
        };

        if (!isAllowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            String errorMessage = switch (requestPath) {
                case "/api/users/login" -> "Too many login attempts. Please try again later.";
                case "/api/users/register" -> "Too many registration attempts. Please try again later.";
                default -> "Too many requests. Please try again later.";
            };

            response.getWriter().write(String.format(
                    "{\"error\": \"%s\", \"status\": %d}",
                    errorMessage,
                    HttpStatus.TOO_MANY_REQUESTS.value()));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip rate limiting for static resources and health checks
        String path = request.getRequestURI();
        return path.startsWith("/static/") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.equals("/actuator/health");
    }
}
