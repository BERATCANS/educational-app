package com.beratcan.app.scheduler;

import com.beratcan.app.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Her gece yarısı tüm öğrencilerin günlük çözülen soru sayısını sıfırlar
 * Cron expression: "0 0 0 * * *" = Her gün 00:00:00'da
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailySolvedQuestionsResetScheduler {

    private final StudentProfileRepository studentProfileRepository;

    /**
     * Her gece yarısı (00:00:00) çalışarak tüm öğrencilerin
     * günlük çözülen soru sayısını sıfırlar
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Istanbul")
    @Transactional
    public void resetAllDailySolvedQuestions() {
        try {
            log.info("Starting daily solved questions reset for all students.");

            int updatedCount = studentProfileRepository.resetAllDailySolvedQuestions();

            log.info("Successfully reset daily solved questions for {} students.", updatedCount);
        } catch (Exception e) {
            log.error("Error resetting daily solved questions: {}", e.getMessage(), e);
        }
    }
}
