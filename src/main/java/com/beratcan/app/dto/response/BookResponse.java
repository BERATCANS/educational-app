package com.beratcan.app.dto.response;

import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.FieldType;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.entity.enums.LessonType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter
public class BookResponse {
    private Long id;
    private String title;
    private String publisher;
    private String coverImageUrl;
    private boolean paid;
    private Integer numberOfTopics;
    private Integer numberOfTests;
    private Integer numberOfQuestions;
    private GradeLevel gradeLevel;
    private FieldType fieldType;
    private ExamType examType;
    private LessonType lessonType;
    private BigDecimal price;
}
