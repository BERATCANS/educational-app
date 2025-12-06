package com.beratcan.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TestRequest {
    @NotBlank(message = "Test başlığı boş olamaz")
    @Size(max = 200, message = "Test başlığı en fazla 200 karakter olabilir")
    private String title;

    @Min(value = 1, message = "Sıra numarası en az 1 olmalıdır")
    private Integer sequenceNumber; // Opsiyonel, belirtilmezse otomatik atanır
}
