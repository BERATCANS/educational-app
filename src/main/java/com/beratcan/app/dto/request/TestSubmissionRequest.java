package com.beratcan.app.dto.request;

import com.beratcan.app.entity.enums.OptionLetter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class TestSubmissionRequest {

    // Not: userId Authentication'dan alınıyor, DTO'da olmasına gerek yok
    // Ama backward compatibility için bırakıyoruz
    private Long userId;

    @NotNull(message = "Test ID'si boş olamaz")
    @Min(value = 1, message = "Test ID'si en az 1 olmalıdır")
    private Long testId;

    private Map<Long, OptionLetter> userAnswers; // questionId -> selectedAnswer

    @NotNull(message = "Toplam süre boş olamaz")
    @Min(value = 0, message = "Toplam süre negatif olamaz")
    private Integer totalTimeSeconds;

}
