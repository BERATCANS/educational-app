package com.beratcan.app.entity;

import com.beratcan.app.entity.enums.PurchaseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // Satın alma anındaki fiyat (indirim öncesi)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_code_id")
    private DiscountCode discountCode; // Kullanılan indirim kodu (varsa)

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount; // Uygulanan indirim tutarı

    @Column(precision = 10, scale = 2)
    private BigDecimal finalPrice; // İndirim sonrası ödenen tutar

    @Column(nullable = false)
    private LocalDateTime purchaseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseStatus status;

    @Column(unique = true, length = 100)
    private String transactionId; // Ödeme işlem numarası (unique - duplicate önleme için)

    private String paymentMethod; // Kredi kartı, havale, vb. (isteğe bağlı)

    @PrePersist
    protected void onCreate() {
        if (purchaseDate == null) {
            purchaseDate = LocalDateTime.now();
        }
        if (status == null) {
            status = PurchaseStatus.PENDING;
        }
    }
}
