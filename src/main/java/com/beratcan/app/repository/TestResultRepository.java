package com.beratcan.app.repository;

import com.beratcan.app.entity.TestResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TestResultRepository extends JpaRepository<TestResult, Long> {

        /**
         * Kullanıcının tüm test sonuçlarını pagination ile getirir
         * DB seviyesinde pagination yapar, memory'ye yük bindirmez
         * JOIN FETCH ile N+1 query problemini önler
         */
        @Query("SELECT tr FROM TestResult tr " +
                        "JOIN FETCH tr.test t " +
                        "JOIN FETCH t.topic tp " +
                        "JOIN FETCH tp.book b " +
                        "WHERE tr.user.id = :userId " +
                        "ORDER BY tr.completedAt DESC")
        Page<TestResult> findByUserIdOrderByCompletedAtDesc(
                        @Param("userId") Long userId,
                        Pageable pageable);

        // Kullanıcının belirli bir test için en son sonucu (Optional)
        Optional<TestResult> findFirstByUserIdAndTestIdOrderByCompletedAtDesc(Long userId, Long testId);

        // Kullanıcının genel istatistikleri
        @Query("SELECT AVG(tr.successRate) FROM TestResult tr WHERE tr.user.id = :userId")
        Double findAverageSuccessRateByUserId(@Param("userId") Long userId);

        @Query("SELECT COUNT(tr) FROM TestResult tr WHERE tr.user.id = :userId")
        Long countTestsByUserId(@Param("userId") Long userId);

        // Kullanıcının belirli bir kitap için çözülen test sayısı
        @Query("SELECT COUNT(DISTINCT tr.test.id) FROM TestResult tr " +
                        "JOIN tr.test t " +
                        "JOIN t.topic tp " +
                        "JOIN tp.book b " +
                        "WHERE tr.user.id = :userId AND b.id = :bookId")
        Long countTestsByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);

        // Kullanıcının belirli bir kitap için en son test çözme tarihi
        @Query("SELECT MAX(tr.completedAt) FROM TestResult tr " +
                        "JOIN tr.test t " +
                        "JOIN t.topic tp " +
                        "JOIN tp.book b " +
                        "WHERE tr.user.id = :userId AND b.id = :bookId")
        java.time.LocalDateTime findLastCompletedDateByUserIdAndBookId(@Param("userId") Long userId,
                        @Param("bookId") Long bookId);

        /**
         * Kullanıcının belirli bir kitaptaki test sonuçlarını pagination ile getirir
         * DB seviyesinde filtreleme yapar, memory'ye yük bindirmez
         * JOIN FETCH ile N+1 query problemini önler
         */
        @Query("SELECT tr FROM TestResult tr " +
                        "JOIN FETCH tr.test t " +
                        "JOIN FETCH t.topic tp " +
                        "JOIN FETCH tp.book b " +
                        "WHERE tr.user.id = :userId AND b.id = :bookId " +
                        "ORDER BY tr.completedAt DESC")
        Page<TestResult> findByUserIdAndBookId(
                        @Param("userId") Long userId,
                        @Param("bookId") Long bookId,
                        Pageable pageable);

        @Query("SELECT COALESCE(SUM(tr.totalQuestions),0) FROM TestResult tr " +
                        "WHERE tr.user.id = :userId AND tr.completedAt BETWEEN :start AND :end")
        Long sumSolvedQuestionsBetween(
                        @Param("userId") Long userId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);
}
