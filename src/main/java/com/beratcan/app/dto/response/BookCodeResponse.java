package com.beratcan.app.dto.response;

import lombok.Data;

@Data
public class BookCodeResponse {
    private Long id;
    private String code;
    private boolean used;
    private Long bookId;
    private String bookTitle;
    private Long usedByUserId;
    private String usedByUsername;
}
