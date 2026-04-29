package com.beratcan.app.repository;

import com.beratcan.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByUsername(String username);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    boolean existsByUsernameAndIdNot(String username, Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByUsername(String username);

    // Email doğrulama için
    Optional<User> findByEmailVerificationToken(String token);

    // Şifre sıfırlama için
    Optional<User> findByPasswordResetToken(String token);

    /**
     * One-row EXISTS lookup over the user_books join table — does this user
     * own this specific book? Avoids hydrating the entire ownedBooks Set.
     */
    boolean existsByIdAndOwnedBooks_Id(Long userId, Long bookId);
}
