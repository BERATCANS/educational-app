package com.beratcan.app.repository;

import com.beratcan.app.entity.Purchase;
import com.beratcan.app.entity.enums.PurchaseStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    // Kullanıcının tüm satın almalarını getir (N+1 query önleme için @EntityGraph)
    @EntityGraph(attributePaths = { "user", "book", "book.publisher", "discountCode" })
    @Query("SELECT p FROM Purchase p WHERE p.user.id = :userId ORDER BY p.purchaseDate DESC")
    List<Purchase> findByUserIdOrderByPurchaseDateDesc(Long userId);

    // Kullanıcının belirli bir kitap için satın almasını kontrol et
    @EntityGraph(attributePaths = { "user", "book", "book.publisher", "discountCode" })
    Optional<Purchase> findByUserIdAndBookIdAndStatus(Long userId, Long bookId, PurchaseStatus status);

    // Kullanıcının tamamlanmış satın almalarını getir (N+1 query önleme için
    // @EntityGraph)
    @EntityGraph(attributePaths = { "user", "book", "book.publisher", "discountCode" })
    @Query("SELECT p FROM Purchase p WHERE p.user.id = :userId AND p.status = :status ORDER BY p.purchaseDate DESC")
    List<Purchase> findByUserIdAndStatusOrderByPurchaseDateDesc(Long userId, PurchaseStatus status);

    // Bir kitabın toplam satış sayısı
    @Query("SELECT COUNT(p) FROM Purchase p WHERE p.book.id = :bookId AND p.status = 'COMPLETED'")
    Long countCompletedPurchasesByBookId(Long bookId);

    /**
     * Purchase'ı User, Book, Publisher ve DiscountCode ile birlikte getirir (JOIN
     * FETCH)
     * LazyInitializationException önlemek için
     */
    @Query("SELECT p FROM Purchase p " +
            "JOIN FETCH p.user u " +
            "JOIN FETCH p.book b " +
            "LEFT JOIN FETCH b.publisher pub " +
            "LEFT JOIN FETCH p.discountCode d " +
            "WHERE p.id = :purchaseId")
    Optional<Purchase> findByIdWithUserAndBook(@Param("purchaseId") Long purchaseId);
}
