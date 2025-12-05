package com.beratcan.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PublisherRequest {

    @NotBlank(message = "Publisher adı boş olamaz")
    private String name;

    @Size(max = 5000, message = "Açıklama en fazla 5000 karakter olabilir")
    private String description;
}
