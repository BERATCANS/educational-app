package com.beratcan.app.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BookReviewsPageResponse extends PageResponse<BookReviewResponse> {

    public BookReviewsPageResponse(
            java.util.List<BookReviewResponse> content,
            int page, int size, long totalElements) {
        super(content, page, size, totalElements,
                (int) Math.ceil((double) totalElements / size),
                page < (int) Math.ceil((double) totalElements / size) - 1,
                page > 0);
    }
}
