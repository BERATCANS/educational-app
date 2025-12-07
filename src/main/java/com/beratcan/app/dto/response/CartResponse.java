package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartResponse {
    private Long id;
    private Long userId;
    private List<CartItemResponse> items;
    private BigDecimal totalPrice; // Toplam fiyat
    private BigDecimal discountAmount; // Uygulanan indirim tutarı
    private BigDecimal totalPriceAfterDiscount; // İndirim sonrası toplam
    private String appliedDiscountCode; // Uygulanan indirim kodu
    private int itemCount; // Toplam ürün sayısı
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String infoMessage; // Bilgilendirme mesajı (error değil)
}
