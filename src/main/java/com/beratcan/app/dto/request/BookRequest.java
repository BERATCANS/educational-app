package com.beratcan.app.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class BookRequest {

    private String title;
    private String publisher;
    private String coverImageUrl;
    private boolean paid;
    private List<String> topics;
}
