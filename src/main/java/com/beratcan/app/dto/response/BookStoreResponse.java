package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookStoreResponse {
    private Long id;
    private String title;
    private String coverImageUrl;
    private String publisher;
    private boolean paid;
    private BigDecimal price;
    private Double averageOverallRating;
    private Double averageContentQualityRating;
    private Double averageDifficultyRating;
    private Double averageVideoQualityRating;
    private boolean isOwned;
    private Long reviewCount;
}
