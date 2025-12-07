package com.beratcan.app.dto.request;

import com.beratcan.app.entity.enums.OptionLetter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DailyQuestionAnswerRequest {

    @NotNull(message = "Soru ID'si boş olamaz")
    @Min(value = 1, message = "Soru ID'si en az 1 olmalıdır")
    private Long dailyQuestionId;

    @NotNull(message = "Seçilen cevap boş olamaz")
    private OptionLetter selectedAnswer;
}
