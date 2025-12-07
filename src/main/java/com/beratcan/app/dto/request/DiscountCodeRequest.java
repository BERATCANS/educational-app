package com.beratcan.app.dto.request;

import com.beratcan.app.entity.enums.DiscountType;
import com.beratcan.app.validation.ValidDiscountCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ValidDiscountCode
public class DiscountCodeRequest {
    @NotBlank(message = "İndirim kodu boş olamaz")
    @Size(max = 50, message = "İndirim kodu en fazla 50 karakter olabilir")
    private String code;

    @NotBlank(message = "Açıklama boş olamaz")
    @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
    private String description;

    @NotNull(message = "İndirim türü boş olamaz")
    private DiscountType discountType;

    @NotNull(message = "İndirim değeri boş olamaz")
    @DecimalMin(value = "0.0", message = "İndirim değeri negatif olamaz")
    private BigDecimal discountValue;

    @NotNull(message = "Kullanım limiti boş olamaz")
    @Min(value = 1, message = "Kullanım limiti en az 1 olmalıdır")
    private Integer usageLimit;

    @DecimalMin(value = "0.0", message = "Minimum sepet tutarı negatif olamaz")
    private BigDecimal minCartTotal;

    @DecimalMin(value = "0.0", message = "Maksimum sepet tutarı negatif olamaz")
    private BigDecimal maxCartTotal;

    @NotNull(message = "Geçerlilik başlangıç tarihi boş olamaz")
    private LocalDateTime validFrom;

    @NotNull(message = "Geçerlilik bitiş tarihi boş olamaz")
    private LocalDateTime validUntil;

    private Boolean isActive;

    @Min(value = 1, message = "Yayınevi ID en az 1 olmalıdır")
    private Long applicablePublisherId; // Yayınevi ID (opsiyonel)

    private List<Long> applicableBookIds; // Kitap ID'leri (opsiyonel)

    @Min(value = 1, message = "Kullanıcı başına limit en az 1 olmalıdır")
    private Integer maxUsagePerUser; // Kullanıcı başına limit (opsiyonel)
}
