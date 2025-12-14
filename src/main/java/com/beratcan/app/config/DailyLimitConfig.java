package com.beratcan.app.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Daily Limit Configuration
 * Kullanıcı başına günlük limit bucket'larını yönetir
 * Rate limit mekanizmasına benzer ama günlük reset ile
 */
@Configuration
public class DailyLimitConfig {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // Günlük limit kapasitesi (default: 20)
    private static final int DEFAULT_DAILY_LIMIT = 20;

    /**
     * Kullanıcı için günlük limit bucket oluştur/getir
     * 
     * @param userId     Kullanıcı ID
     * @param dailyLimit Günlük limit (default: 20)
     * @return Bucket
     */
    public Bucket resolveDailyLimitBucket(Long userId, int dailyLimit) {
        String key = "daily_limit_user_" + userId;
        return cache.computeIfAbsent(key, k -> createDailyLimitBucket(dailyLimit));
    }

    /**
     * Kullanıcı için günlük limit bucket oluştur/getir (default limit ile)
     * 
     * @param userId Kullanıcı ID
     * @return Bucket
     */
    public Bucket resolveDailyLimitBucket(Long userId) {
        return resolveDailyLimitBucket(userId, DEFAULT_DAILY_LIMIT);
    }

    /**
     * Yeni günlük limit bucket oluştur
     * Token bucket algoritması: dailyLimit token, her 24 saatte refill
     * 
     * @param dailyLimit Günlük limit
     * @return Bucket
     */
    private Bucket createDailyLimitBucket(int dailyLimit) {
        // Token bucket: dailyLimit tokens, her 24 saatte refill
        // Not: Bu refill mekanizması son kullanımdan itibaren 24 saat sonra çalışır
        // Tam gece yarısı reset için scheduler kullanılacak
        Bandwidth limit = Bandwidth.builder()
                .capacity(dailyLimit)
                .refillIntervally(dailyLimit, Duration.ofDays(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Kullanıcının günlük limit bucket'ını reset et (gece yarısı için)
     * 
     * @param userId Kullanıcı ID
     */
    public void resetDailyLimitBucket(Long userId) {
        String key = "daily_limit_user_" + userId;
        cache.remove(key);
    }

    /**
     * Tüm günlük limit bucket'larını temizle (gece yarısı için)
     */
    public void clearAllDailyLimitBuckets() {
        cache.clear();
    }

    /**
     * Cache'deki bucket sayısı
     * 
     * @return Bucket sayısı
     */
    public int getCacheSize() {
        return cache.size();
    }
}
