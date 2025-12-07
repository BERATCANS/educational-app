package com.beratcan.app.aspect;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.config.RateLimitConfig;
import com.beratcan.app.exception.TooManyRequestsException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Rate Limiting Aspect
 * 
 * @RateLimit annotation'ını handle eder
 *            Method execution'dan ÖNCE rate limit kontrolü yapar
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitConfig rateLimitConfig;

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            // Request context yoksa (test senaryosu gibi), rate limit kontrolü yapma
            return joinPoint.proceed();
        }

        String key = getRateLimitKey(request, rateLimit.keyType());
        Bucket bucket = getBucket(key, rateLimit.keyType(), rateLimit.requestsPerMinute());

        // Try to consume 1 token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            // Rate limit exceeded
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000; // seconds

            log.warn("Rate limit exceeded for endpoint: key={}, method={}, limit={}/min, retryAfter={}s",
                    key, joinPoint.getSignature().getName(), rateLimit.requestsPerMinute(), waitForRefill);

            throw new TooManyRequestsException(
                    rateLimit.message(),
                    waitForRefill);
        }

        log.debug("Rate limit check passed: key={}, remaining={}, method={}",
                key, probe.getRemainingTokens(), joinPoint.getSignature().getName());

        // Response header'larına remaining tokens ekle
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                var response = attributes.getResponse();
                if (response != null) {
                    response.setHeader("X-Rate-Limit-Remaining",
                            String.valueOf(probe.getRemainingTokens()));
                }
            }
        } catch (Exception e) {
            // Response header ekleme başarısız olursa devam et
            log.debug("Could not set rate limit header: {}", e.getMessage());
        }

        return joinPoint.proceed();
    }

    /**
     * Rate limit key'ini oluşturur (IP, USER, veya GLOBAL)
     */
    private String getRateLimitKey(HttpServletRequest request, String keyType) {
        if ("USER".equals(keyType)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                try {
                    return "user_" + auth.getName();
                } catch (Exception e) {
                    // Fallback to IP
                    return "ip_" + getClientIP(request);
                }
            }
            // Not authenticated, fallback to IP
            return "ip_" + getClientIP(request);
        } else if ("GLOBAL".equals(keyType)) {
            return "global";
        } else {
            // IP bazlı (default)
            return "ip_" + getClientIP(request);
        }
    }

    /**
     * Bucket'ı alır veya oluşturur
     */
    private Bucket getBucket(String key, String keyType, int requestsPerMinute) {
        if (key.startsWith("user_")) {
            try {
                Long userId = Long.parseLong(key.substring(5));
                return rateLimitConfig.resolveBucketByUserId(userId, requestsPerMinute);
            } catch (NumberFormatException e) {
                // Fallback to IP
                String ip = key.substring(3);
                return rateLimitConfig.resolveBucket(ip, requestsPerMinute);
            }
        } else if (key.startsWith("ip_")) {
            String ip = key.substring(3);
            return rateLimitConfig.resolveBucket(ip, requestsPerMinute);
        } else {
            // GLOBAL - IP bazlı kullan
            return rateLimitConfig.resolveBucket("global", requestsPerMinute);
        }
    }

    /**
     * Client IP adresini alır
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    /**
     * Current HTTP request'i alır
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
