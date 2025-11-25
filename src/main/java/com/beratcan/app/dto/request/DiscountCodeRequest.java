package com.beratcan.app.dto.request;

import com.beratcan.app.entity.enums.DiscountType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DiscountCodeRequest {
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Integer usageLimit;
    private BigDecimal minCartTotal;
    private BigDecimal maxCartTotal;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean isActive;
    private Long applicablePublisherId; // Yayınevi ID (opsiyonel)
    private List<Long> applicableBookIds; // Kitap ID'leri (opsiyonel)
    private Integer maxUsagePerUser; // Kullanıcı başına limit (opsiyonel)
}
