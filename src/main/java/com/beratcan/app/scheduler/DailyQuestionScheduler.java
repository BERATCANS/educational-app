package com.beratcan.app.scheduler;

import com.beratcan.app.entity.DailyQuestion;
import com.beratcan.app.repository.DailyQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyQuestionScheduler {

    private final DailyQuestionRepository dailyQuestionRepository;

    // Her dakika çalışarak aktif/pasif durumunu kontrol et
    @Scheduled(fixedRate = 60000) // 60 saniye
    @Transactional
    public void checkAndUpdateQuestionStatus() {
        log.info("🔄 Checking daily question status...");
        LocalDate today = LocalDate.now();
        log.info("📅 Today's date: {}", today);
        try {
            dailyQuestionRepository.findByDisplayDate(today).ifPresent(question -> {
                log.info("📝 Found question for today. ID: {}, Start: {}, End: {}", question.getId(), question.getStartTime(), question.getEndTime());
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = LocalDateTime.of(question.getDisplayDate(), question.getStartTime());
            LocalDateTime endTime = LocalDateTime.of(question.getDisplayDate(), question.getEndTime());

            boolean shouldBeActive = now.isAfter(startTime) && now.isBefore(endTime);
            log.info("⏰ Current time: {}, Should be active: {}", now, shouldBeActive);
            
            if (question.isActive() != shouldBeActive) {
                question.setActive(shouldBeActive);
                dailyQuestionRepository.save(question);
                log.info("Daily question status updated. ID: {}, Active: {}", question.getId(), shouldBeActive);
            }
            });
        } catch (Exception e) {
            log.error("❌ Error checking question status: {}", e.getMessage(), e);
        }
    }

    // Her gün gece yarısı çalışarak bir sonraki günün sorusunu aktive et
    @Scheduled(cron = "0 0 0 * * *") // Her gün 00:00'da
    @Transactional
    public void activateNextDayQuestion() {
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            dailyQuestionRepository.findByDisplayDate(tomorrow).ifPresent(question -> {
                log.info("Next day's question prepared. ID: {}, Date: {}", question.getId(), tomorrow);
            });
        } catch (Exception e) {
            log.error("❌ Error activating next day question: {}", e.getMessage(), e);
        }
    }
}
