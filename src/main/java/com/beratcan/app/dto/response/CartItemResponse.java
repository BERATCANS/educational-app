package com.beratcan.app.dto.response;

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
public class CartItemResponse {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookPublisher;
    private String bookCoverImageUrl;
    private BigDecimal priceAtAdd;
    private BigDecimal currentPrice; // Güncel fiyat (fiyat değişmiş olabilir)
    private LocalDateTime addedAt;
    private boolean isPriceChanged; // Fiyat değişti mi kontrolü
}
