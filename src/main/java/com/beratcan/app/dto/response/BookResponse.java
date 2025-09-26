package com.beratcan.app.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class BookResponse {
    private Long id;
    private String title;
    private String publisher;
    private String coverImageUrl;
    private boolean paid;
}
