package com.beratcan.app.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class TopicRequest {
    private String title;
    private List<String> tests; // Opsiyonel, test eklemek için
    private Integer sequenceNumber; // Opsiyonel, belirtilmezse otomatik atanır
}
