package com.beratcan.app.scheduler;

import com.beratcan.app.config.DailyLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Her gece yarısı tüm kullanıcıların günlük limit bucket'larını sıfırlar
 * Cron expression: "0 0 0 * * *" = Her gün 00:00:00'da
 * 
 * Rate limit mekanizmasına benzer ama günlük reset ile
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyLimitResetScheduler {

    private final DailyLimitConfig dailyLimitConfig;

    /**
     * Her gece yarısı (00:00:00) çalışarak tüm kullanıcıların
     * günlük limit bucket'larını temizler
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Istanbul") // Her gün Türkiye saatiyle 00:00:00'da
    public void resetAllDailyLimitBuckets() {
        try {
            int cacheSizeBefore = dailyLimitConfig.getCacheSize();
            log.info("🔄 Starting daily limit bucket reset. Cache size before: {}", cacheSizeBefore);

            // Tüm günlük limit bucket'larını temizle
            dailyLimitConfig.clearAllDailyLimitBuckets();

            log.info("✅ Successfully reset all daily limit buckets. Cleared {} buckets.", cacheSizeBefore);
        } catch (Exception e) {
            log.error("❌ Error resetting daily limit buckets: {}", e.getMessage(), e);
        }
    }
}
