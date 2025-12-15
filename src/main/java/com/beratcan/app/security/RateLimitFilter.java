package com.beratcan.app.security;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.config.RateLimitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Rate Limiting Filter
 * Her request'i rate limit kontrolünden geçirir
 * IP bazlı ve USER bazlı rate limiting destekler
 * 
 * Order(1) ile JWT filter'dan ÖNCE çalışır
 */
@Slf4j
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;
    private final RequestMappingHandlerMapping handlerMapping;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitConfig rateLimitConfig,
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            ObjectMapper objectMapper) {
        this.rateLimitConfig = rateLimitConfig;
        this.handlerMapping = handlerMapping;
        this.objectMapper = objectMapper;
    }

    // Default: 500 requests per minute (genel endpoint'ler için - toplam limit)
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 500;

    // Endpoint-specific rate limits
    private static final int LOGIN_RATE_LIMIT = 5; // 5 login per minute per IP
    private static final int REGISTER_RATE_LIMIT = 3; // 3 register per minute per IP
    private static final int REFRESH_RATE_LIMIT = 10; // 10 refresh per minute per IP
    private static final int AI_UPLOAD_RATE_LIMIT = 10; // 5 AI upload per minute per USER
    private static final int TEST_SUBMIT_RATE_LIMIT = 10; // 5 test submit per minute per USER

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // OPTIONS request'leri rate limit'e tabi değil (CORS preflight)
        if ("OPTIONS".equals(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Annotation kontrolü: Eğer endpoint'te @RateLimit varsa, Filter skip et
        // (Aspect zaten kontrol edecek)
        if (hasRateLimitAnnotation(request)) {
            log.debug("Skipping rate limit filter for path={} (has @RateLimit annotation)", path);
            filterChain.doFilter(request, response);
            return;
        }

        // Rate limit kontrolü (sadece annotation olmayan endpoint'ler için)
        RateLimitResult rateLimitResult = checkRateLimit(request, path);

        if (rateLimitResult.isAllowed()) {
            // ✅ Request allowed
            if (rateLimitResult.getRemainingTokens() != null) {
                response.addHeader("X-Rate-Limit-Remaining",
                        String.valueOf(rateLimitResult.getRemainingTokens()));
            }
            filterChain.doFilter(request, response);
        } else {
            // ❌ Rate limit exceeded
            handleRateLimitExceeded(response, rateLimitResult, path);
        }
    }

    /**
     * Request'in gideceği handler method'unda @RateLimit annotation'ı var mı
     * kontrol eder
     * 
     * @param request HTTP request
     * @return true eğer @RateLimit annotation'ı varsa
     */
    private boolean hasRateLimitAnnotation(HttpServletRequest request) {
        try {
            // HandlerMapping'den handler method'unu bul
            HandlerMethod handlerMethod = getHandlerMethod(request);
            if (handlerMethod != null) {
                // Method'da @RateLimit annotation'ı var mı?
                return handlerMethod.hasMethodAnnotation(RateLimit.class);
            }
        } catch (Exception e) {
            // Handler bulunamazsa veya hata olursa, annotation yok say
            log.debug("Could not check @RateLimit annotation: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Request'in gideceği handler method'unu bulur
     */
    private HandlerMethod getHandlerMethod(HttpServletRequest request) {
        try {
            // HandlerMapping'den handler'ı al
            var handlerExecutionChain = handlerMapping.getHandler(request);
            if (handlerExecutionChain != null) {
                Object handler = handlerExecutionChain.getHandler();
                if (handler instanceof HandlerMethod) {
                    return (HandlerMethod) handler;
                }
            }
        } catch (Exception e) {
            // Handler bulunamazsa null döndür
            log.debug("Handler not found for request: {}", request.getRequestURI());
        }
        return null;
    }

    /**
     * Rate limit kontrolü yapar
     */
    private RateLimitResult checkRateLimit(HttpServletRequest request, String path) {
        String ip = getClientIP(request);
        int rateLimit = getRateLimitForPath(path);
        String keyType = getKeyTypeForPath(path);

        Bucket bucket;

        if ("USER".equals(keyType)) {
            // USER bazlı rate limiting (authenticated users için)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                try {
                    Long userId = Long.parseLong(auth.getName());
                    bucket = rateLimitConfig.resolveBucketByUserId(userId, rateLimit);
                    log.debug("Rate limit check (USER): userId={}, path={}, limit={}/min",
                            userId, path, rateLimit);
                } catch (NumberFormatException e) {
                    // Fallback to IP if userId parse fails
                    bucket = rateLimitConfig.resolveBucket(ip, rateLimit);
                    log.debug("Rate limit check (IP fallback): ip={}, path={}, limit={}/min",
                            ip, path, rateLimit);
                }
            } else {
                // Not authenticated, use IP-based limiting
                bucket = rateLimitConfig.resolveBucket(ip, rateLimit);
                log.debug("Rate limit check (IP - not authenticated): ip={}, path={}, limit={}/min",
                        ip, path, rateLimit);
            }
        } else {
            // IP bazlı rate limiting
            bucket = rateLimitConfig.resolveBucket(ip, rateLimit);
            log.debug("Rate limit check (IP): ip={}, path={}, limit={}/min", ip, path, rateLimit);
        }

        // Try to consume 1 token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        return new RateLimitResult(
                probe.isConsumed(),
                probe.getRemainingTokens(),
                probe.getNanosToWaitForRefill());
    }

    /**
     * Path'e göre rate limit değerini döndürür
     */
    private int getRateLimitForPath(String path) {
        if (path.startsWith("/api/users/login")) {
            return LOGIN_RATE_LIMIT;
        } else if (path.startsWith("/api/users/register")) {
            return REGISTER_RATE_LIMIT;
        } else if (path.startsWith("/api/users/refresh")) {
            return REFRESH_RATE_LIMIT;
        } else if (path.startsWith("/api/files/upload-question-with-ai")) {
            return AI_UPLOAD_RATE_LIMIT;
        } else if (path.startsWith("/api/test-results/submit")) {
            return TEST_SUBMIT_RATE_LIMIT;
        }
        return DEFAULT_REQUESTS_PER_MINUTE;
    }

    /**
     * Path'e göre key type'ı döndürür (IP veya USER)
     */
    private String getKeyTypeForPath(String path) {
        // USER bazlı rate limiting gereken endpoint'ler
        if (path.startsWith("/api/files/upload-question-with-ai") ||
                path.startsWith("/api/test-results/submit")) {
            return "USER";
        }
        // Diğerleri IP bazlı
        return "IP";
    }

    /**
     * Rate limit aşıldığında response döndürür
     */
    private void handleRateLimitExceeded(HttpServletResponse response,
            RateLimitResult rateLimitResult,
            String path) throws IOException {
        long waitForRefill = rateLimitResult.getNanosToWaitForRefill() / 1_000_000_000; // seconds

        log.warn("Rate limit exceeded: path={}, retryAfter={}s", path, waitForRefill);

        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json");
        response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Too Many Requests");
        errorResponse.put("message", "Rate limit exceeded. Please try again later.");
        errorResponse.put("retryAfterSeconds", waitForRefill);
        errorResponse.put("status", 429);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Client IP adresini alır (proxy header'larını kontrol eder)
     */
    private String getClientIP(HttpServletRequest request) {
        // Check for proxy headers
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        // First IP is the original client
        return xfHeader.split(",")[0].trim();
    }

    /**
     * Rate limit kontrol sonucu
     */
    private static class RateLimitResult {
        private final boolean allowed;
        private final Long remainingTokens;
        private final long nanosToWaitForRefill;

        public RateLimitResult(boolean allowed, Long remainingTokens, long nanosToWaitForRefill) {
            this.allowed = allowed;
            this.remainingTokens = remainingTokens;
            this.nanosToWaitForRefill = nanosToWaitForRefill;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public Long getRemainingTokens() {
            return remainingTokens;
        }

        public long getNanosToWaitForRefill() {
            return nanosToWaitForRefill;
        }
    }
}
