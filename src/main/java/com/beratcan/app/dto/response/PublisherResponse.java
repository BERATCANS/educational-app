package com.beratcan.app.dto.response;

import lombok.Data;

@Data
public class PublisherResponse {
    private Long id;
    private String name;
    private String description;
    private long bookCount;
}
