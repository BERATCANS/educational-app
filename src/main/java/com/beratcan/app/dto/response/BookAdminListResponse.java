package com.beratcan.app.dto.response;

import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.entity.enums.LessonType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Admin paneli liste/detay endpoint'lerinde dönen Book temsili.
 * Mağaza endpoint'inden farklı olarak: enum tipler raw (label değil) gönderilir
 * ki düzenleme modalı select'leri direkt eşleştirebilsin; description ve
 * yapısal sayaçlar dahildir.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookAdminListResponse {
    private Long id;
    private String title;
    private Long publisherId;
    private String publisher;
    private String coverImageUrl;
    private boolean paid;
    private BigDecimal price;
    private String description;
    private GradeLevel gradeLevel;
    private ExamType examType;
    private LessonType lessonType;
    private Integer numberOfTopics;
    private Integer numberOfTests;
    private Integer numberOfQuestions;
}
