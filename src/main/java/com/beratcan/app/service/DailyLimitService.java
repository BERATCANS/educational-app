package com.beratcan.app.service;

import com.beratcan.app.config.DailyLimitConfig;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.DailyLimitExceededException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Daily Limit Service
 * Kullanıcı başına günlük limit kontrolü yapar
 * AI konuşmaları için kullanılır (text + speech toplam 20/gün)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyLimitService {

    private final DailyLimitConfig dailyLimitConfig;

    // Günlük AI konuşma limiti
    private static final int DAILY_AI_CONVERSATION_LIMIT = 20;

    /**
     * Kullanıcının günlük AI konuşma limitini kontrol et
     * 
     * @param userId Kullanıcı ID
     * @throws BadRequestException Limit aşıldıysa
     */
    public void checkDailyAiConversationLimit(Long userId) {
        Bucket bucket = dailyLimitConfig.resolveDailyLimitBucket(userId, DAILY_AI_CONVERSATION_LIMIT);

        // 1 token tüketmeyi dene (sadece kontrol için)
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            // Limit aşıldı
            long remaining = probe.getRemainingTokens();
            log.warn("Daily AI conversation limit exceeded: userId={}, limit={}, remaining={}",
                    userId, DAILY_AI_CONVERSATION_LIMIT, remaining);

            throw new DailyLimitExceededException(
                    "Günlük AI konuşma limitinize ulaştınız. Limitiniz günlük " +
                            DAILY_AI_CONVERSATION_LIMIT + " konuşmadır. Yarın tekrar deneyebilirsiniz.");
        }

        log.debug("Daily AI conversation limit check passed: userId={}, remaining={}",
                userId, probe.getRemainingTokens());
    }

    /**
     * Kullanıcının günlük AI konuşma sayacını artır
     * Not: Bucket4j'in token mekanizması zaten token'ı tükettiği için
     * bu metod artık gerekli değil, checkDailyAiConversationLimit zaten token'ı
     * tüketir
     * 
     * @param userId Kullanıcı ID
     */
    @Deprecated
    public void incrementDailyAiConversations(Long userId) {
        // Token zaten checkDailyAiConversationLimit'te tüketildi
        // Bu metod deprecated, sadece geriye dönük uyumluluk için tutuluyor
        log.debug("Daily AI conversation incremented: userId={}", userId);
    }

    /**
     * Kullanıcının kalan günlük AI konuşma sayısını getir
     * 
     * @param userId Kullanıcı ID
     * @return Kalan konuşma sayısı
     */
    public long getRemainingDailyAiConversations(Long userId) {
        Bucket bucket = dailyLimitConfig.resolveDailyLimitBucket(userId, DAILY_AI_CONVERSATION_LIMIT);
        return bucket.getAvailableTokens();
    }
}
