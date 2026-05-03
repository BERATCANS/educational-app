package com.beratcan.app.dto.response;

import com.beratcan.app.entity.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private Integer maxUsagePerUser;
    private List<PublisherSummary> applicablePublishers;
    private List<BookSummary> applicableBooks;
    private List<String> applicableUsernames;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @AllArgsConstructor
    public static class PublisherSummary {
        private Long id;
        private String name;
    }

    @Data
    @AllArgsConstructor
    public static class BookSummary {
        private Long id;
        private String title;
    }
}
