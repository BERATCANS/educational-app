package com.beratcan.app.dto.response;

import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.entity.enums.LessonType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookDetailResponse {
    private Long id;
    private String title;
    private String publisher;
    private boolean paid;
    private Integer numberOfTopics;
    private Integer numberOfTests;
    private Integer numberOfQuestions;
    private GradeLevel gradeLevel;
    private ExamType examType;
    private LessonType lessonType;
    private BigDecimal price;
    private String description;
    private boolean isOwned;
    private Double averageRating;
    private Long reviewCount;
    private List<BookReviewResponse> reviews;
}
