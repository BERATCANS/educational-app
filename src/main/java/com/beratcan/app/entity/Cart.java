package com.beratcan.app.entity;

import com.beratcan.app.constants.AppTime;
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
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version; // Optimistic locking için version field

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false, unique = true)
    private StudentProfile studentProfile;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_code_id")
    private DiscountCode appliedDiscountCode; // Sepette uygulanan kod

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount; // Uygulanan indirim tutarı

    @Column(precision = 10, scale = 2)
    private BigDecimal totalPriceAfterDiscount; // İndirim sonrası toplam

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = AppTime.now();
        updatedAt = AppTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = AppTime.now();
    }

    // Helper metotlar
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }
}
