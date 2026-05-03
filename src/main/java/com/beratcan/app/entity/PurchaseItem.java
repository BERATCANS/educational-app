package com.beratcan.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "purchase_items")
public class PurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // Satın alma anındaki fiyat (indirim öncesi)

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount; // Bu kitaba düşen indirim tutarı

    @Column(precision = 10, scale = 2)
    private BigDecimal finalPrice; // İndirim sonrası bu kitap için ödenen tutar
}
