package com.beratcan.app.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private final Map<String, RateLimitInfo> rateLimits = new ConcurrentHashMap<>();

    // Rate limiting for login attempts: 5 attempts per minute
    public boolean isLoginAllowed(String clientIp) {
        return isAllowed(clientIp, 5, 60); // 5 attempts per 60 seconds
    }

    // Rate limiting for registration attempts: 3 attempts per 10 minutes
    public boolean isRegistrationAllowed(String clientIp) {
        return isAllowed(clientIp + "_register", 3, 600); // 3 attempts per 600 seconds
    }

    // Rate limiting for general API calls: 100 requests per minute
    public boolean isApiCallAllowed(String clientIp) {
        return isAllowed(clientIp + "_api", 100, 60); // 100 requests per 60 seconds
    }

    private boolean isAllowed(String key, int maxAttempts, int windowSeconds) {
        LocalDateTime now = LocalDateTime.now();
        RateLimitInfo info = rateLimits.computeIfAbsent(key, k -> new RateLimitInfo());

        // Reset if window has passed
        if (info.getFirstAttempt() != null &&
                info.getFirstAttempt().plusSeconds(windowSeconds).isBefore(now)) {
            info.reset();
        }

        // Check if limit exceeded
        if (info.getAttemptCount() >= maxAttempts) {
            return false;
        }

        // Record this attempt
        info.incrementAttempt();
        if (info.getFirstAttempt() == null) {
            info.setFirstAttempt(now);
        }

        return true;
    }

    // Get remaining tokens for a client
    public long getRemainingTokens(String clientIp, String type) {
        String key = switch (type) {
            case "login" -> clientIp;
            case "register" -> clientIp + "_register";
            case "api" -> clientIp + "_api";
            default -> clientIp;
        };

        RateLimitInfo info = rateLimits.get(key);
        if (info == null) {
            return switch (type) {
                case "login" -> 5;
                case "register" -> 3;
                case "api" -> 100;
                default -> 100;
            };
        }

        int maxAttempts = switch (type) {
            case "login" -> 5;
            case "register" -> 3;
            case "api" -> 100;
            default -> 100;
        };

        return Math.max(0, maxAttempts - info.getAttemptCount());
    }

    // Reset rate limit for a client (admin function)
    public void resetRateLimit(String clientIp) {
        rateLimits.remove(clientIp);
        rateLimits.remove(clientIp + "_register");
        rateLimits.remove(clientIp + "_api");
    }

    private static class RateLimitInfo {
        private int attemptCount = 0;
        private LocalDateTime firstAttempt;

        public void incrementAttempt() {
            attemptCount++;
        }

        public void reset() {
            attemptCount = 0;
            firstAttempt = null;
        }

        public int getAttemptCount() {
            return attemptCount;
        }

        public LocalDateTime getFirstAttempt() {
            return firstAttempt;
        }

        public void setFirstAttempt(LocalDateTime firstAttempt) {
            this.firstAttempt = firstAttempt;
        }
    }
}
