package com.beratcan.app.repository;

import com.beratcan.app.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Sepetteki belirli bir kitabı bul
    Optional<CartItem> findByCartIdAndBookId(Long cartId, Long bookId);

    // Sepette bu kitap var mı kontrol et
    boolean existsByCartIdAndBookId(Long cartId, Long bookId);
}
