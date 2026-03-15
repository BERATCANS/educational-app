package com.beratcan.app.dto.response;

import com.beratcan.app.entity.enums.PurchaseStatus;
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
public class PurchaseResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userUsername;

    // Sipariş toplamları
    private BigDecimal totalPrice;       // Toplam fiyat (indirim öncesi)
    private BigDecimal totalDiscount;    // Toplam indirim tutarı
    private BigDecimal finalPrice;       // İndirim sonrası ödenen toplam
    private int itemCount;               // Kaç kitap satın alındı

    // İndirim bilgileri
    private String discountCode;         // Kullanılan indirim kodu (varsa)

    // Sipariş bilgileri
    private LocalDateTime purchaseDate;
    private PurchaseStatus status;
    private String transactionId;
    private String paymentMethod;
    private String infoMessage;          // Bilgilendirme mesajı (error değil)

    // Sipariş kalemleri
    private List<PurchaseItemResponse> items;
}
