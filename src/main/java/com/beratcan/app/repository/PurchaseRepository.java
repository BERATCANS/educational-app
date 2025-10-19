package com.beratcan.app.repository;

import com.beratcan.app.entity.Purchase;
import com.beratcan.app.entity.enums.PurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    // Kullanıcının tüm satın almalarını getir
    List<Purchase> findByUserIdOrderByPurchaseDateDesc(Long userId);

    // Kullanıcının belirli bir kitap için satın almasını kontrol et
    Optional<Purchase> findByUserIdAndBookIdAndStatus(Long userId, Long bookId, PurchaseStatus status);

    // Kullanıcının tamamlanmış satın almalarını getir
    List<Purchase> findByUserIdAndStatusOrderByPurchaseDateDesc(Long userId, PurchaseStatus status);

    // Bir kitabın toplam satış sayısı
    @Query("SELECT COUNT(p) FROM Purchase p WHERE p.book.id = :bookId AND p.status = 'COMPLETED'")
    Long countCompletedPurchasesByBookId(Long bookId);
}
