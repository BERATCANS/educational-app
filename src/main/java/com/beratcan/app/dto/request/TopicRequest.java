package com.beratcan.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TopicRequest {
    @NotBlank(message = "Konu başlığı boş olamaz")
    @Size(max = 100, message = "Konu başlığı en fazla 100 karakter olabilir")
    private String title;

    private List<String> tests; // Opsiyonel, test eklemek için

    @Min(value = 1, message = "Sıra numarası en az 1 olmalıdır")
    private Integer sequenceNumber; // Opsiyonel, belirtilmezse otomatik atanır
}
