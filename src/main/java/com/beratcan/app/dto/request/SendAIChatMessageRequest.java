package com.beratcan.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendAIChatMessageRequest {
    @NotBlank(message = "Mesaj boş olamaz")
    @Size(max = 2000, message = "Mesaj en fazla 2000 karakter olabilir")
    private String message;
}
