package com.beratcan.app.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DiscountCodeUsageResponse {
    private Long id;
    private Long userId;
    private String username;
    private String email;
    private LocalDateTime usedAt;
    private String transactionId;
}
