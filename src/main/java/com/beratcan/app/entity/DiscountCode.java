package com.beratcan.app.entity;

import com.beratcan.app.constants.AppTime;
import com.beratcan.app.entity.enums.DiscountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Setter
@SQLDelete(sql = "UPDATE discount_codes SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Table(name = "discount_codes", indexes = {
        @Index(name = "idx_discount_code_code", columnList = "code"),
        @Index(name = "idx_discount_code_active", columnList = "is_active")
})
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version; // Optimistic locking için version field - concurrent discount code usage'ı önler

    @Column(nullable = false, unique = true, length = 50)
    private String code; // "YKS2024", "YENI30" gibi

    @Column(nullable = false, length = 500)
    private String description; // "YKS kitapları için %30 indirim"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType; // PERCENTAGE, FIXED_AMOUNT

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue; // %30 için 30, 50 TL için 50

    // Kullanım limiti
    @Column(nullable = false)
    private Integer usageLimit; // Toplam kullanım sayısı

    @Column(nullable = false)
    private Integer usedCount = 0; // Kullanılan sayı

    // Fiyat aralığı (opsiyonel)
    @Column(precision = 10, scale = 2)
    private BigDecimal minCartTotal; // Minimum sepet tutarı

    @Column(precision = 10, scale = 2)
    private BigDecimal maxCartTotal; // Maksimum sepet tutarı

    // Tarih aralığı
    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    // Aktif mi?
    @Column(nullable = false, name = "is_active")
    private Boolean isActive = true;

    // Özel kısıtlamalar
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "discount_code_publishers",
            joinColumns = @JoinColumn(name = "discount_code_id"),
            inverseJoinColumns = @JoinColumn(name = "publisher_id"))
    private Set<Publisher> applicablePublishers = new HashSet<>(); // boş ise tüm yayınevleri

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "discount_code_books", joinColumns = @JoinColumn(name = "discount_code_id"), inverseJoinColumns = @JoinColumn(name = "book_id"))
    private Set<Book> applicableBooks = new HashSet<>(); // null/empty ise tüm kitaplar

    // Kullanıcı başına limit (opsiyonel)
    @Column
    private Integer maxUsagePerUser; // null ise sınırsız

    // Kullanıcıya özel kod (opsiyonel)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "discount_code_users",
            joinColumns = @JoinColumn(name = "discount_code_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> applicableUsers = new HashSet<>(); // boş ise herkese açık

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = AppTime.now();
        updatedAt = AppTime.now();
        if (usedCount == null) {
            usedCount = 0;
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = AppTime.now();
    }

    @Column(nullable = false)
    private boolean isDeleted = false; // Soft Delete mantığı için
}
