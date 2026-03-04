package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookReviewResponse {
    private Long id;
    private Long userId;
    private String userName;
    private int overallRating;
    private int contentQualityRating;
    private int difficultyRating;
    private int videoQualityRating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isMyReview; // Kullanıcının kendi yorumu mu?
}
