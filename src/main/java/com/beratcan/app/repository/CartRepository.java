package com.beratcan.app.repository;

import com.beratcan.app.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Kullanıcının sepetini getir
    Optional<Cart> findByUserId(Long userId);

    // Kullanıcının sepeti var mı kontrol et
    boolean existsByUserId(Long userId);
}
