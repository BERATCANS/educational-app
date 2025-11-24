package com.beratcan.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PublisherRequest {

    @NotBlank(message = "Publisher adı boş olamaz")
    private String name;

    private String description;
}
