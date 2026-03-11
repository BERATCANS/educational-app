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

    // Kullanıcının tüm satın almalarını getir (items ve book'larıyla birlikte)
    @Query("SELECT DISTINCT p FROM Purchase p " +
            "LEFT JOIN FETCH p.items i " +
            "LEFT JOIN FETCH i.book b " +
            "LEFT JOIN FETCH b.publisher " +
            "LEFT JOIN FETCH p.discountCode " +
            "LEFT JOIN FETCH p.user u " +
            "WHERE p.user.id = :userId " +
            "ORDER BY p.purchaseDate DESC")
    List<Purchase> findByUserIdOrderByPurchaseDateDesc(@Param("userId") Long userId);

    // Kullanıcının belirli bir kitap için satın almasını kontrol et (items üzerinden)
    @Query("SELECT DISTINCT p FROM Purchase p " +
            "JOIN p.items i " +
            "WHERE p.user.id = :userId " +
            "AND i.book.id = :bookId " +
            "AND p.status = :status")
    Optional<Purchase> findByUserIdAndBookIdAndStatus(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            @Param("status") PurchaseStatus status);

    // Kullanıcının tamamlanmış satın almalarını getir
    @Query("SELECT DISTINCT p FROM Purchase p " +
            "LEFT JOIN FETCH p.items i " +
            "LEFT JOIN FETCH i.book b " +
            "LEFT JOIN FETCH b.publisher " +
            "LEFT JOIN FETCH p.discountCode " +
            "LEFT JOIN FETCH p.user u " +
            "WHERE p.user.id = :userId AND p.status = :status " +
            "ORDER BY p.purchaseDate DESC")
    List<Purchase> findByUserIdAndStatusOrderByPurchaseDateDesc(
            @Param("userId") Long userId,
            @Param("status") PurchaseStatus status);

    // Bir kitabın toplam satış sayısı (items üzerinden)
    @Query("SELECT COUNT(i) FROM PurchaseItem i " +
            "WHERE i.book.id = :bookId " +
            "AND i.purchase.status = 'COMPLETED'")
    Long countCompletedPurchasesByBookId(@Param("bookId") Long bookId);

    // Purchase'ı tüm ilişkileriyle birlikte getirir
    @Query("SELECT p FROM Purchase p " +
            "JOIN FETCH p.user u " +
            "LEFT JOIN FETCH p.items i " +
            "LEFT JOIN FETCH i.book b " +
            "LEFT JOIN FETCH b.publisher pub " +
            "LEFT JOIN FETCH p.discountCode d " +
            "WHERE p.id = :purchaseId")
    Optional<Purchase> findByIdWithUserAndBook(@Param("purchaseId") Long purchaseId);

    // Admin Listeleme ve Dinamik Filtreleme Sorgusu
    @Query("SELECT DISTINCT p FROM Purchase p " +
           "LEFT JOIN p.items i " +
           "WHERE (:userId IS NULL OR p.user.id = :userId) AND " +
           "(:bookId IS NULL OR i.book.id = :bookId) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:transactionId IS NULL OR LOWER(p.transactionId) LIKE LOWER(CONCAT('%', :transactionId, '%'))) AND " +
           "(:startDate IS NULL OR p.purchaseDate >= :startDate) AND " +
           "(:endDate IS NULL OR p.purchaseDate <= :endDate) AND " +
           "(:minPrice IS NULL OR p.finalPrice >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.finalPrice <= :maxPrice)")
    org.springframework.data.domain.Page<Purchase> searchPurchases(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            @Param("status") PurchaseStatus status,
            @Param("transactionId") String transactionId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            @Param("minPrice") java.math.BigDecimal minPrice,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            org.springframework.data.domain.Pageable pageable);
}
