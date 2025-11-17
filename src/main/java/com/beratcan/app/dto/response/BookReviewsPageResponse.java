package com.beratcan.app.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BookReviewsPageResponse extends PageResponse<BookReviewResponse> {
    private BookReviewResponse ownedReview; // Kullanıcının kendi yorumu (varsa, sadece ilk sayfada)

    public BookReviewsPageResponse(BookReviewResponse ownedReview,
            java.util.List<BookReviewResponse> notOwnedReviews,
            int page, int size, long totalElements) {
        super(notOwnedReviews, page, size, totalElements,
                (int) Math.ceil((double) totalElements / size),
                page < (int) Math.ceil((double) totalElements / size) - 1,
                page > 0);
        this.ownedReview = ownedReview;
    }
}
