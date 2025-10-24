package com.beratcan.app.dto.request;

import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.FieldType;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.entity.enums.LessonType;
import java.math.BigDecimal;
import lombok.Data;
import java.util.List;

@Data
public class BookRequest {

    private String title;
    private String publisher;
    private String coverImageUrl;
    private boolean paid;
    private List<String> topics;
    private GradeLevel gradeLevel;
    private FieldType fieldType;
    private ExamType examType;
    private LessonType lessonType;
    private BigDecimal price;
    private String description;
}
