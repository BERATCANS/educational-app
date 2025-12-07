package com.beratcan.app.repository;

import com.beratcan.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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

    /**
     * Tüm kullanıcıların günlük çözülen soru sayısını sıfırla
     * Her gece yarısı scheduled job tarafından çağrılır
     * 
     * @return Güncellenen kullanıcı sayısı
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.dailySolvedQuestions = 0")
    int resetAllDailySolvedQuestions();
}