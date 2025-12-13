package com.beratcan.app.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendAIChatMessageRequest {
    /**
     * Text mesaj (voice mesaj için boş olabilir)
     */
    @Size(max = 2000, message = "Mesaj en fazla 2000 karakter olabilir")
    private String message;
}
