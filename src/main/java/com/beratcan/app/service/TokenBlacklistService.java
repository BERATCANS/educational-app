package com.beratcan.app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token blacklist servisi - Logout ve güvenlik için
 * Kullanıcı logout olduğunda token'ı blacklist'e ekler
 * Expired token'ları otomatik temizler
 * 
 * NOT: Production'da Redis kullanımı önerilir
 */
@Service
@Slf4j
public class TokenBlacklistService {

    // In-memory storage for blacklisted tokens
    // Key: token, Value: expiration date
    private final ConcurrentHashMap<String, Date> blacklistedTokens = new ConcurrentHashMap<>();

    /**
     * Token'ı blacklist'e ekle
     * 
     * @param token      JWT token
     * @param expiration Token'ın expire tarihi
     */
    public void blacklistToken(String token, Date expiration) {
        blacklistedTokens.put(token, expiration);
        log.info("Token blacklisted. Current blacklist size: {}", blacklistedTokens.size());
    }

    /**
     * Token blacklist'te mi kontrol et
     * 
     * @param token JWT token
     * @return true ise token blacklist'te
     */
    public boolean isTokenBlacklisted(String token) {
        Date expiration = blacklistedTokens.get(token);
        if (expiration == null) {
            return false;
        }

        // Token expire olmuşsa blacklist'ten çıkar
        if (expiration.before(new Date())) {
            blacklistedTokens.remove(token);
            log.debug("Expired token removed from blacklist");
            return false;
        }

        return true;
    }

    /**
     * Expire olmuş token'ları temizle
     * Her 5 dakikada bir otomatik çalışır
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void cleanupExpiredTokens() {
        Date now = new Date();
        int beforeSize = blacklistedTokens.size();

        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().before(now));

        int afterSize = blacklistedTokens.size();
        if (beforeSize != afterSize) {
            log.info("Cleaned up {} expired tokens from blacklist. Remaining: {}",
                    beforeSize - afterSize, afterSize);
        }
    }

    /**
     * Tüm blacklist'i temizle (test için)
     */
    public void clearAllTokens() {
        blacklistedTokens.clear();
        log.warn("All blacklisted tokens cleared");
    }

    /**
     * Blacklist'teki token sayısı
     * 
     * @return token sayısı
     */
    public int getBlacklistedTokenCount() {
        cleanupExpiredTokens();
        return blacklistedTokens.size();
    }
}
