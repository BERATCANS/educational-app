package com.beratcan.app.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

/**
 * Geçici ses dosyalarını temizleyen zamanlanmış görev.
 * 
 * Voice chat streaming sırasında oluşturulan ve "audio/temp/" altında saklanan
 * geçici dosyaları periyodik olarak siler.
 */
@Slf4j
@Component
public class AudioCleanupScheduler {

    @Value("${audio.temp.dir:uploads/audio/temp/}")
    private String tempAudioDir;

    // 1 Saatlik (3600 sn) ömür
    private static final long FILE_AGE_THRESHOLD_HOURS = 1;

    /**
     * Her 30 dakikada bir çalışır (fixedRate = 1800000 ms)
     * 1 saatten eski dosyaları siler.
     */
    @Scheduled(fixedRate = 1800000)
    public void cleanupTempAudioFiles() {
        log.info("🧹 Audio cleanup başladı: {}", tempAudioDir);
        
        Path dirPath = Paths.get(tempAudioDir);
        if (!Files.exists(dirPath)) {
            log.debug("Temp audio dizini mevcut değil, işlem atlanıyor.");
            return;
        }

        try (Stream<Path> files = Files.list(dirPath)) {
            files.forEach(this::checkAndDeleteFile);
        } catch (IOException e) {
            log.error("Audio cleanup sırasında dizin okuma hatası: {}", e.getMessage());
        }
    }

    private void checkAndDeleteFile(Path file) {
        if (!Files.isRegularFile(file)) return;

        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            Instant lastModified = attrs.lastModifiedTime().toInstant();
            Instant now = Instant.now();

            long hoursOld = ChronoUnit.HOURS.between(lastModified, now);

            if (hoursOld >= FILE_AGE_THRESHOLD_HOURS) {
                Files.delete(file); // Dosyayı sil
                log.info("🗑️ Eski audio dosyası silindi: {} ({} saat eski)", file.getFileName(), hoursOld);
            }
        } catch (IOException e) {
            log.error("Dosya silinirken hata oluştu: {} ({})", file.getFileName(), e.getMessage());
        }
    }
}
