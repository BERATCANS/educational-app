package com.beratcan.app.dto.request;

import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.entity.enums.LessonType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tek istekle Book + Topics + Tests hiyerarşisini oluşturmak için DTO.
 * Görsel yok — book cover ayrı endpoint ile yüklenir.
 */
@Data
public class BulkCreateBookRequest {

    @NotBlank(message = "Kitap adı boş olamaz")
    @Size(max = 200, message = "Kitap adı en fazla 200 karakter olabilir")
    private String title;

    @Min(value = 1, message = "publisherId en az 1 olmalıdır")
    private Long publisherId;

    private String publisher;

    @NotNull(message = "Sınıf seviyesi boş olamaz")
    private GradeLevel gradeLevel;

    @NotNull(message = "Sınav türü boş olamaz")
    private ExamType examType;

    @NotNull(message = "Ders türü boş olamaz")
    private LessonType lessonType;

    @Size(max = 2000, message = "Açıklama en fazla 2000 karakter olabilir")
    private String description;

    private boolean paid;

    @DecimalMin(value = "0.0", message = "Fiyat negatif olamaz")
    private BigDecimal price;

    @NotEmpty(message = "En az bir konu eklenmelidir")
    @Valid
    private List<TopicItem> topics;

    @Data
    public static class TopicItem {
        @NotBlank(message = "Konu başlığı boş olamaz")
        @Size(max = 100, message = "Konu başlığı en fazla 100 karakter olabilir")
        private String title;

        @Min(value = 1, message = "Sıra numarası en az 1 olmalıdır")
        private Integer sequenceNumber;

        @Valid
        private List<TestItem> tests;
    }

    @Data
    public static class TestItem {
        @NotBlank(message = "Test başlığı boş olamaz")
        @Size(max = 200, message = "Test başlığı en fazla 200 karakter olabilir")
        private String title;

        @Min(value = 1, message = "Sıra numarası en az 1 olmalıdır")
        private Integer sequenceNumber;
    }
}
