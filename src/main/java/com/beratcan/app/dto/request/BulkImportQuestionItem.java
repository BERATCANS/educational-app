package com.beratcan.app.dto.request;

import com.beratcan.app.entity.enums.OptionLetter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Bulk question import için tek bir soru kaydı.
 * imageFilename ile multipart'taki images[] listesindeki dosyaya bağlanır.
 */
@Data
public class BulkImportQuestionItem {

    @NotBlank(message = "Görsel dosya adı boş olamaz")
    private String imageFilename;

    @NotNull(message = "Doğru cevap boş olamaz")
    private OptionLetter correctAnswer;

    @NotNull(message = "Cevap sayısı boş olamaz")
    @Min(value = 2, message = "Cevap sayısı en az 2 olmalıdır")
    @Max(value = 10, message = "Cevap sayısı en fazla 10 olabilir")
    private Integer answerNumber;

    @Min(value = 1, message = "Sıra numarası en az 1 olmalıdır")
    private Integer sequenceNumber;

    @Size(max = 500, message = "Video URL'si en fazla 500 karakter olabilir")
    private String videoUrl;

    // hint1/2/3 ve solution opsiyonel: eksik gelirse o alan boş bırakılır
    // ve aiAnalysisStatus FAILED olur (admin sonradan tamamlayabilir).
    private String hint1;

    private String hint2;

    private String hint3;

    private String solution;
}
