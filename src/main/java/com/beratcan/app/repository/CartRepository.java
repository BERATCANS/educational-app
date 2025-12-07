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

    // Kullanıcının sepetini getir (pessimistic lock ile - concurrent checkout'u
    // önlemek için)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdWithLock(@Param("userId") Long userId);

    // Kullanıcının sepetini getir (normal okuma için)
    Optional<Cart> findByUserId(Long userId);

    // Kullanıcının sepeti var mı kontrol et
    boolean existsByUserId(Long userId);
}
