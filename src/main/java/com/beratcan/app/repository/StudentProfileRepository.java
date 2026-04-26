package com.beratcan.app.repository;

import com.beratcan.app.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Optional<StudentProfile> findByParentInviteCode(String parentInviteCode);

    boolean existsByParentInviteCode(String parentInviteCode);

    /**
     * Öğrencinin flaggedQuestions koleksiyonuyla birlikte getirir (JOIN FETCH)
     * LazyInitializationException önlemek için
     */
    @Query("SELECT DISTINCT sp FROM StudentProfile sp " +
            "LEFT JOIN FETCH sp.flaggedQuestions fq " +
            "LEFT JOIN FETCH fq.test t " +
            "LEFT JOIN FETCH t.topic tp " +
            "LEFT JOIN FETCH tp.book b " +
            "LEFT JOIN FETCH fq.hints h " +
            "LEFT JOIN FETCH fq.explanation e " +
            "WHERE sp.user.id = :userId")
    Optional<StudentProfile> findByUserIdWithFlaggedQuestions(@Param("userId") Long userId);

    /**
     * Tüm öğrencilerin günlük çözülen soru sayısını sıfırla
     * Her gece yarısı scheduler tarafından çağrılır
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StudentProfile sp SET sp.dailySolvedQuestions = 0")
    int resetAllDailySolvedQuestions();
}
