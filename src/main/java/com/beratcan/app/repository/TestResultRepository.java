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
         * Öğrencinin tüm test sonuçlarını pagination ile getirir
         */
        @Query("SELECT tr FROM TestResult tr " +
                        "JOIN FETCH tr.test t " +
                        "JOIN FETCH t.topic tp " +
                        "JOIN FETCH tp.book b " +
                        "WHERE tr.studentProfile.user.id = :userId " +
                        "ORDER BY tr.completedAt DESC")
        Page<TestResult> findByUserIdOrderByCompletedAtDesc(
                        @Param("userId") Long userId,
                        Pageable pageable);

        /**
         * Öğrencinin tüm test sonuçlarını pagination ile getirir (QuestionResults ile)
         */
        @Query("SELECT DISTINCT tr FROM TestResult tr " +
                        "JOIN FETCH tr.test t " +
                        "JOIN FETCH t.topic tp " +
                        "JOIN FETCH tp.book b " +
                        "LEFT JOIN FETCH tr.questionResults qr " +
                        "LEFT JOIN FETCH qr.question q " +
                        "WHERE tr.studentProfile.user.id = :userId " +
                        "ORDER BY tr.completedAt DESC")
        Page<TestResult> findByUserIdOrderByCompletedAtDescWithQuestionResults(
                        @Param("userId") Long userId,
                        Pageable pageable);

        // Öğrencinin belirli bir test için en son sonucu (studentProfileId ile)
        Optional<TestResult> findFirstByStudentProfileIdAndTestIdOrderByCompletedAtDesc(
                        Long studentProfileId, Long testId);

        /**
         * TestResult'ı Test ve StudentProfile ile birlikte getirir (JOIN FETCH)
         */
        @Query("SELECT tr FROM TestResult tr " +
                        "JOIN FETCH tr.test t " +
                        "JOIN FETCH tr.studentProfile sp " +
                        "JOIN FETCH sp.user u " +
                        "WHERE tr.id = :testResultId")
        Optional<TestResult> findByIdWithTestAndUser(@Param("testResultId") Long testResultId);

        /**
         * TestResult'ı Test, StudentProfile ve QuestionResults ile birlikte getirir
         */
        @Query("SELECT DISTINCT tr FROM TestResult tr " +
                        "JOIN FETCH tr.test t " +
                        "JOIN FETCH tr.studentProfile sp " +
                        "JOIN FETCH sp.user u " +
                        "LEFT JOIN FETCH tr.questionResults qr " +
                        "LEFT JOIN FETCH qr.question q " +
                        "WHERE tr.id = :testResultId")
        Optional<TestResult> findByIdWithTestUserAndQuestionResults(@Param("testResultId") Long testResultId);

        /**
         * Öğrencinin belirli bir test için en son sonucunu detaylı getirir
         */
        @Query("SELECT DISTINCT tr FROM TestResult tr " +
                        "JOIN FETCH tr.test t " +
                        "JOIN FETCH tr.studentProfile sp " +
                        "JOIN FETCH sp.user u " +
                        "LEFT JOIN FETCH tr.questionResults qr " +
                        "LEFT JOIN FETCH qr.question q " +
                        "WHERE sp.user.id = :userId AND tr.test.id = :testId " +
                        "ORDER BY tr.completedAt DESC")
        java.util.List<TestResult> findFirstByUserIdAndTestIdWithTestAndUser(
                        @Param("userId") Long userId,
                        @Param("testId") Long testId);

        // Öğrencinin genel istatistikleri
        @Query("SELECT AVG(tr.successRate) FROM TestResult tr WHERE tr.studentProfile.user.id = :userId")
        Double findAverageSuccessRateByUserId(@Param("userId") Long userId);

        @Query("SELECT COUNT(tr) FROM TestResult tr WHERE tr.studentProfile.user.id = :userId")
        Long countTestsByUserId(@Param("userId") Long userId);

        // Öğrencinin belirli bir kitap için çözülen test sayısı
        @Query("SELECT COUNT(DISTINCT tr.test.id) FROM TestResult tr " +
                        "JOIN tr.test t " +
                        "JOIN t.topic tp " +
                        "JOIN tp.book b " +
                        "WHERE tr.studentProfile.user.id = :userId AND b.id = :bookId")
        Long countTestsByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);

        // Öğrencinin belirli bir kitap için en son test çözme tarihi
        @Query("SELECT MAX(tr.completedAt) FROM TestResult tr " +
                        "JOIN tr.test t " +
                        "JOIN t.topic tp " +
                        "JOIN tp.book b " +
                        "WHERE tr.studentProfile.user.id = :userId AND b.id = :bookId")
        java.time.LocalDateTime findLastCompletedDateByUserIdAndBookId(@Param("userId") Long userId,
                        @Param("bookId") Long bookId);

        /**
         * Öğrencinin belirli bir kitaptaki test sonuçlarını pagination ile getirir
         */
        @Query("SELECT tr FROM TestResult tr " +
                        "JOIN FETCH tr.test t " +
                        "JOIN FETCH t.topic tp " +
                        "JOIN FETCH tp.book b " +
                        "WHERE tr.studentProfile.user.id = :userId AND b.id = :bookId " +
                        "ORDER BY tr.completedAt DESC")
        Page<TestResult> findByUserIdAndBookId(
                        @Param("userId") Long userId,
                        @Param("bookId") Long bookId,
                        Pageable pageable);

        /**
         * Öğrencinin belirli bir kitaptaki test sonuçlarını getirir (pagination yok)
         */
        @Query("SELECT tr FROM TestResult tr " +
                        "JOIN FETCH tr.test t " +
                        "JOIN FETCH t.topic tp " +
                        "JOIN FETCH tp.book b " +
                        "WHERE tr.studentProfile.user.id = :userId AND b.id = :bookId " +
                        "ORDER BY tr.completedAt DESC")
        java.util.List<TestResult> findAllByUserIdAndBookId(
                        @Param("userId") Long userId,
                        @Param("bookId") Long bookId);

        @Query("SELECT COALESCE(SUM(tr.totalQuestions),0) FROM TestResult tr " +
                        "WHERE tr.studentProfile.user.id = :userId AND tr.completedAt BETWEEN :start AND :end")
        Long sumSolvedQuestionsBetween(
                        @Param("userId") Long userId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);
}
