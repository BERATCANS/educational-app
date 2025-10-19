package com.beratcan.app.dto.response;

import com.beratcan.app.entity.enums.PurchaseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long bookId;
    private String bookTitle;
    private String bookCoverImageUrl;
    private BigDecimal price;
    private LocalDateTime purchaseDate;
    private PurchaseStatus status;
    private String transactionId;
    private String paymentMethod;
}
