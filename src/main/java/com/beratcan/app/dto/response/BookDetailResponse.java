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
public class BookDetailResponse {
    private Long id;
    private String title;
    private String publisher;
    private String coverImageUrl;
    private boolean paid;
    private Integer numberOfTopics;
    private Integer numberOfTests;
    private Integer numberOfQuestions;
    private String gradeLevelLabel;
    private String examTypeLabel;
    private String lessonTypeLabel;
    private BigDecimal price;
    private String description;
    private boolean isOwned;
    private Double averageOverallRating;
    private Double averageContentQualityRating;
    private Double averageDifficultyRating;
    private Double averageVideoQualityRating;
    private Long reviewCount;
}
