package com.beratcan.app.dto.response;

import com.beratcan.app.entity.enums.DiscountType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DiscountCodeResponse {
    private Long id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Integer usageLimit;
    private Integer usedCount;
    private BigDecimal minCartTotal;
    private BigDecimal maxCartTotal;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean isActive;
    private String applicablePublisherName;
    private Integer applicableBookCount;
    private Integer maxUsagePerUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
