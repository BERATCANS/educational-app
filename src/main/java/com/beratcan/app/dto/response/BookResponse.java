package com.beratcan.app.dto.response;

import lombok.Data;


@Data
public class BookResponse {
    private Long id;
    private String title;
    private String publisher;
    private String coverImageUrl;
    private boolean paid;
}
