package com.beratcan.app.scheduler;

import com.beratcan.app.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Geçici ses dosyalarını temizleyen zamanlanmış görev.
 * 
 * Voice chat streaming sırasında oluşturulan ve "audio/temp/" altında saklanan
 * geçici dosyaları periyodik olarak siler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AudioCleanupScheduler {

    private final StorageService storageService;

    // StorageService içinde prefix olarak kullanılıyor
    private static final String AUDIO_TEMP_PREFIX = "audio/temp/";

    // 1 Saatlik (3600 sn) ömür
    private static final long FILE_AGE_THRESHOLD_HOURS = 1;

    /**
     * Her 30 dakikada bir çalışır (fixedRate = 1800000 ms)
     * 1 saatten eski dosyaları siler.
     */
    @Scheduled(fixedRate = 1800000)
    public void cleanupTempAudioFiles() {
        log.info("🧹 Audio cleanup başladı: {}", AUDIO_TEMP_PREFIX);
        
        try {
            storageService.deleteOldFiles(AUDIO_TEMP_PREFIX, FILE_AGE_THRESHOLD_HOURS);
            log.info("Audio cleanup tamamlandı.");
        } catch (Exception e) {
            log.error("Audio cleanup sırasında hata: {}", e.getMessage());
        }
    }
}
