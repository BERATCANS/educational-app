package com.beratcan.app.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookReviewRequest {
    private int rating;
    private String comment;
}
