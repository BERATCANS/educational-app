package com.beratcan.app.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Rate Limiting Configuration
 * IP ve USER bazlı rate limit bucket'larını yönetir
 * Spring Cache kullanarak in-memory storage sağlar
 */
@Configuration
public class RateLimitConfig {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public RateLimitConfig() {
    }

    /**
     * IP için rate limit bucket oluştur/getir
     *
     * @param ip                IP adresi
     * @param requestsPerMinute Dakikada izin verilen request sayısı
     * @return Bucket
     */
    public Bucket resolveBucket(String ip, int requestsPerMinute) {
        String key = "rate_limit_ip_" + ip;
        return cache.computeIfAbsent(key, k -> createNewBucket(requestsPerMinute));
    }

    /**
     * Kullanıcı ID bazlı bucket (authenticated users için)
     *
     * @param userId            Kullanıcı ID
     * @param requestsPerMinute Dakikada izin verilen request sayısı
     * @return Bucket
     */
    public Bucket resolveBucketByUserId(Long userId, int requestsPerMinute) {
        String key = "rate_limit_user_" + userId;
        return cache.computeIfAbsent(key, k -> createNewBucket(requestsPerMinute));
    }

    /**
     * Yeni bucket oluştur
     * Token bucket algoritması: requestsPerMinute token, her dakika refill
     *
     * @param requestsPerMinute Dakikada izin verilen request sayısı
     * @return Bucket
     */
    private Bucket createNewBucket(int requestsPerMinute) {
        // Token bucket: requestsPerMinute tokens, her dakika refill
        Bandwidth limit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillIntervally(requestsPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Cache'i temizle (opsiyonel - memory management için)
     */
    public void clearCache() {
        cache.clear();
    }
}
