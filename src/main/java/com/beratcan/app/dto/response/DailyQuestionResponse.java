package com.beratcan.app.dto.response;

import com.beratcan.app.entity.enums.OptionLetter;
import com.beratcan.app.entity.DailyQuestion;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@Data
public class DailyQuestionResponse {
    public DailyQuestionResponse() {}

    public DailyQuestionResponse(DailyQuestion question, String errorMessage) {
        if (question != null) {
            this.id = question.getId();
            this.imageUrl = question.getImageUrl();
            this.answerNumber = question.getAnswerNumber();
            this.videoUrl = question.getVideoUrl();
            this.displayDate = question.getDisplayDate();
            this.startTime = question.getStartTime();
            this.endTime = question.getEndTime();
            this.active = question.isActive();
        }
        if (errorMessage != null) {
            this.errorMessage = errorMessage;
        }
    }
    private Long id;
    private String imageUrl;
    private Integer answerNumber;
    private String videoUrl;
    private LocalDate displayDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean active;
    private boolean hasUserAnswered;
    private UserAnswerInfo userAnswer;
    private AfterEndTimeInfo afterEndTime;
    private String errorMessage;

    @Data
    public static class UserAnswerInfo {
        private OptionLetter selectedAnswer;
        private String answeredAt;
    }

    @Data
    public static class AfterEndTimeInfo {
        private OptionLetter correctAnswer;
        private String explanation;
        private AnswerStatistics statistics;
    }

    @Data
    public static class AnswerStatistics {
        private long totalAnswers;
        private Map<OptionLetter, Long> distribution;
    }
}
