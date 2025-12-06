package com.beratcan.app.dto.request;

import com.beratcan.app.entity.enums.OptionLetter;
import com.beratcan.app.validation.ValidImageFile;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateDailyQuestionRequest {
    @NotNull(message = "Görsel dosyası boş olamaz")
    @ValidImageFile(message = "Geçersiz görsel dosyası")
    private MultipartFile image;

    @NotNull(message = "Cevap sayısı boş olamaz")
    @Min(value = 2, message = "Cevap sayısı en az 2 olmalıdır")
    @Max(value = 10, message = "Cevap sayısı en fazla 10 olabilir")
    private Integer answerNumber;

    @NotNull(message = "Doğru cevap boş olamaz")
    private OptionLetter correctAnswer;

    @Size(max = 500, message = "Video URL'si en fazla 500 karakter olabilir")
    private String videoUrl;

    private String explanation; // TEXT alanı, sınırsız

    @NotNull(message = "Gösterim tarihi boş olamaz")
    private LocalDate displayDate;

    @NotNull(message = "Başlangıç saati boş olamaz")
    private LocalTime startTime;

    @NotNull(message = "Bitiş saati boş olamaz")
    private LocalTime endTime;
}
