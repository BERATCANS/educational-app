package com.beratcan.app.entity;

import com.beratcan.app.constants.AppTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "discount_code_usages", indexes = {
        @Index(name = "idx_discount_code_usage_code", columnList = "discount_code_id"),
        @Index(name = "idx_discount_code_usage_user", columnList = "user_id"),
        @Index(name = "idx_discount_code_usage_date", columnList = "used_at")
})
public class DiscountCodeUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_code_id", nullable = false)
    private DiscountCode discountCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime usedAt;

    @Column(length = 100)
    private String transactionId; // Hangi satın alma işleminde kullanıldı

    @PrePersist
    protected void onCreate() {
        if (usedAt == null) {
            usedAt = AppTime.now();
        }
    }
}
