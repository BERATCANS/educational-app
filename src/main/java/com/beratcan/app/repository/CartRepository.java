package com.beratcan.app.repository;

import com.beratcan.app.entity.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Öğrencinin sepetini getir (pessimistic lock ile)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.studentProfile.user.id = :userId")
    Optional<Cart> findByUserIdWithLock(@Param("userId") Long userId);

    // Kullanıcının sepetini getir (normal okuma için)
    @Query("SELECT c FROM Cart c WHERE c.studentProfile.user.id = :userId")
    Optional<Cart> findByUserId(@Param("userId") Long userId);

    /**
     * Öğrencinin sepetini tüm ilişkilerle birlikte getirir (JOIN FETCH)
     */
    @Query("SELECT DISTINCT c FROM Cart c " +
            "JOIN FETCH c.studentProfile sp " +
            "JOIN FETCH sp.user u " +
            "LEFT JOIN FETCH c.items i " +
            "LEFT JOIN FETCH i.book b " +
            "LEFT JOIN FETCH b.publisher p " +
            "LEFT JOIN FETCH c.appliedDiscountCode d " +
            "WHERE u.id = :userId")
    Optional<Cart> findByUserIdWithItemsAndBooks(@Param("userId") Long userId);

    /**
     * Öğrencinin sepetini pessimistic lock ile tüm ilişkilerle birlikte getirir
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT c FROM Cart c " +
            "JOIN FETCH c.studentProfile sp " +
            "JOIN FETCH sp.user u " +
            "LEFT JOIN FETCH c.items i " +
            "LEFT JOIN FETCH i.book b " +
            "LEFT JOIN FETCH b.publisher p " +
            "LEFT JOIN FETCH c.appliedDiscountCode d " +
            "WHERE u.id = :userId")
    Optional<Cart> findByUserIdWithLockAndItems(@Param("userId") Long userId);

    @Query("SELECT COUNT(c) > 0 FROM Cart c WHERE c.studentProfile.user.id = :userId")
    boolean existsByUserId(@Param("userId") Long userId);
}
