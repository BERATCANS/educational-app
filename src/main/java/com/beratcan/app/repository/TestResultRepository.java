package com.beratcan.app.repository;

import com.beratcan.app.entity.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    // Kullanıcının tüm test sonuçları
    List<TestResult> findByUserIdOrderByCompletedAtDesc(Long userId);

    // Kullanıcının belirli bir test için sonuçları
    List<TestResult> findByUserIdAndTestId(Long userId, Long testId);

    // Kullanıcının belirli bir test için en son sonucu (Optional)
    Optional<TestResult> findFirstByUserIdAndTestIdOrderByCompletedAtDesc(Long userId, Long testId);

    // Kullanıcının en son test sonucu
    Optional<TestResult> findFirstByUserIdOrderByCompletedAtDesc(Long userId);

    // Kullanıcının genel istatistikleri
    @Query("SELECT AVG(tr.successRate) FROM TestResult tr WHERE tr.user.id = :userId")
    Double findAverageSuccessRateByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(tr) FROM TestResult tr WHERE tr.user.id = :userId")
    Long countTestsByUserId(@Param("userId") Long userId);

    // Book ID'den tüm test ID'lerini bul
    @Query("SELECT DISTINCT t.id FROM Test t " +
            "JOIN t.topic tp " +
            "JOIN tp.book b " +
            "WHERE b.id = :bookId")
    List<Long> findTestIdsByBookId(@Param("bookId") Long bookId);
}
