package com.beratcan.app.entity;

import com.beratcan.app.entity.enums.PurchaseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice; // Toplam fiyat (indirim öncesi)

    @Column(precision = 10, scale = 2)
    private BigDecimal totalDiscount; // Toplam indirim tutarı

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal finalPrice; // İndirim sonrası ödenen toplam tutar

    @Column(nullable = false)
    private int itemCount; // Kaç kitap satın alındı

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_code_id")
    private DiscountCode discountCode; // Kullanılan indirim kodu (varsa)

    @Column(nullable = false)
    private LocalDateTime purchaseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseStatus status;

    @Column(length = 100)
    private String transactionId; // Benzersiz işlem numarası

    @Column(length = 50)
    private String paymentMethod;

    @PrePersist
    protected void onCreate() {
        if (purchaseDate == null) {
            purchaseDate = LocalDateTime.now();
        }
        if (status == null) {
            status = PurchaseStatus.PENDING;
        }
    }

    // Helper metotlar
    public void addItem(PurchaseItem item) {
        items.add(item);
        item.setPurchase(this);
    }
}
