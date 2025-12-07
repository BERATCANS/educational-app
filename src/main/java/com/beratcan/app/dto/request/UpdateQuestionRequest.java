package com.beratcan.app.dto.request;

import com.beratcan.app.entity.enums.OptionLetter;
import com.beratcan.app.validation.ValidImageFile;

import jakarta.validation.constraints.Max;

import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.Size;

import lombok.Data;

import org.springframework.web.multipart.MultipartFile;

@Data

public class UpdateQuestionRequest {

    // Opsiyonel: Yeni görsel yüklenecekse
    @ValidImageFile(message = "Geçersiz görsel dosyası")
    private MultipartFile image;

    // Opsiyonel: Sadece değiştirilecek alanlar
    private OptionLetter correctAnswer;

    @Min(value = 2, message = "Cevap sayısı en az 2 olmalıdır")
    @Max(value = 10, message = "Cevap sayısı en fazla 10 olabilir")
    private Integer answerNumber;

    @Size(max = 500, message = "Video URL'si en fazla 500 karakter olabilir")
    private String videoUrl;

    @Min(value = 1, message = "Sıra numarası en az 1 olmalıdır")
    private Integer sequenceNumber;

}
