package com.beratcan.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedbackRequest {

    @NotBlank(message = "Başlık boş olamaz")
    @Size(max = 200, message = "Başlık en fazla 200 karakter olabilir")
    private String title;

    @NotBlank(message = "Mesaj boş olamaz")
    @Size(max = 5000, message = "Mesaj en fazla 5000 karakter olabilir")
    private String text;
}
