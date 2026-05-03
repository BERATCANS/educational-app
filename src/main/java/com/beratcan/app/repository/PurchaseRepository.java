package com.beratcan.app.repository;

import com.beratcan.app.entity.Purchase;
import com.beratcan.app.entity.enums.PurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase> {

    // Kullanıcının tüm satın almalarını getir
    @Query("SELECT DISTINCT p FROM Purchase p " +
            "LEFT JOIN FETCH p.items i " +
            "LEFT JOIN FETCH i.book b " +
            "LEFT JOIN FETCH b.publisher " +
            "LEFT JOIN FETCH p.discountCode " +
            "JOIN FETCH p.studentProfile sp " +
            "JOIN FETCH sp.user u " +
            "WHERE u.id = :userId " +
            "ORDER BY p.purchaseDate DESC")
    List<Purchase> findByUserIdOrderByPurchaseDateDesc(@Param("userId") Long userId);

    // Öğrencinin belirli bir kitap için satın almasını kontrol et
    @Query("SELECT DISTINCT p FROM Purchase p " +
            "JOIN p.items i " +
            "JOIN p.studentProfile sp " +
            "JOIN sp.user u " +
            "WHERE u.id = :userId " +
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
            "JOIN FETCH p.studentProfile sp " +
            "JOIN FETCH sp.user u " +
            "WHERE u.id = :userId AND p.status = :status " +
            "ORDER BY p.purchaseDate DESC")
    List<Purchase> findByUserIdAndStatusOrderByPurchaseDateDesc(
            @Param("userId") Long userId,
            @Param("status") PurchaseStatus status);

    // Bir kitabın toplam satış sayısı
    @Query("SELECT COUNT(i) FROM PurchaseItem i " +
            "WHERE i.book.id = :bookId " +
            "AND i.purchase.status = 'COMPLETED'")
    Long countCompletedPurchasesByBookId(@Param("bookId") Long bookId);

    // Purchase'ı tüm ilişkileriyle birlikte getirir.
    // user için LEFT JOIN: soft-delete edilmiş kullanıcıların satın almaları da admin'e dönsün
    // (mapping katmanında "Silinmiş Kullanıcı" placeholder ile işlenecek).
    @Query("SELECT p FROM Purchase p " +
            "JOIN FETCH p.studentProfile sp " +
            "LEFT JOIN FETCH sp.user u " +
            "LEFT JOIN FETCH p.items i " +
            "LEFT JOIN FETCH i.book b " +
            "LEFT JOIN FETCH b.publisher pub " +
            "LEFT JOIN FETCH p.discountCode d " +
            "WHERE p.id = :purchaseId")
    Optional<Purchase> findByIdWithUserAndBook(@Param("purchaseId") Long purchaseId);

    @Query("SELECT p FROM Purchase p " +
            "JOIN FETCH p.studentProfile sp " +
            "JOIN FETCH sp.user u " +
            "LEFT JOIN FETCH p.items i " +
            "LEFT JOIN FETCH i.book b " +
            "LEFT JOIN FETCH b.publisher " +
            "LEFT JOIN FETCH p.discountCode " +
            "WHERE u.id = :userId AND p.idempotencyKey = :key")
    Optional<Purchase> findByUserIdAndIdempotencyKey(
            @Param("userId") Long userId,
            @Param("key") String idempotencyKey);

}
