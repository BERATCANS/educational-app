package com.beratcan.app.dto;

import lombok.Data;

import java.util.List;

@Data
public class BookRequestDTO {

    private String title;
    private String publisher;
    private String coverImageUrl;
    private boolean paid;
    private List<String> codes;
}
