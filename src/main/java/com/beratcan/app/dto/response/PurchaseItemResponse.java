package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseItemResponse {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookCoverImageUrl;
    private String bookPublisher;
    private BigDecimal price;           // İndirim öncesi fiyat
    private BigDecimal discountAmount;  // Bu kitaba düşen indirim
    private BigDecimal finalPrice;      // İndirim sonrası fiyat
}
