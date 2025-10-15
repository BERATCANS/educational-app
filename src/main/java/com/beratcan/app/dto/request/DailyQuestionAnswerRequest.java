package com.beratcan.app.dto.request;

import com.beratcan.app.entity.enums.OptionLetter;
import lombok.Data;

@Data
public class DailyQuestionAnswerRequest {
    private Long dailyQuestionId;
    private OptionLetter selectedAnswer;
}
