package com.beratcan.app.dto.request;

import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.entity.enums.LessonType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BookRequest {

    @NotBlank(message = "Kitap adı boş olamaz")
    @Size(max = 200, message = "Kitap adı en fazla 200 karakter olabilir")
    private String title;

    /**
     * Var olan bir yayınevi kullanılacaksa id gönderilir.
     * publisherId null ise publisher alanındaki isim kullanılarak yeni yayınevi
     * oluşturulur.
     */
    @Min(value = 1, message = "publisherId en az 1 olmalıdır")
    private Long publisherId;

    private String publisher;

    @Size(max = 500, message = "Kapak resmi URL'si en fazla 500 karakter olabilir")
    private String coverImageUrl;

    private boolean paid;

    private List<String> topics;

    @NotNull(message = "Sınıf seviyesi boş olamaz")
    private GradeLevel gradeLevel;

    @NotNull(message = "Sınav türü boş olamaz")
    private ExamType examType;

    @NotNull(message = "Ders türü boş olamaz")
    private LessonType lessonType;

    @DecimalMin(value = "0.0", message = "Fiyat negatif olamaz")
    private BigDecimal price;

    @Size(max = 2000, message = "Açıklama en fazla 2000 karakter olabilir")
    private String description;
}
