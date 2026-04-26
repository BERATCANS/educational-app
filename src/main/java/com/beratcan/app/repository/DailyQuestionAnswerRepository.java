package com.beratcan.app.repository;

import com.beratcan.app.entity.DailyQuestionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyQuestionAnswerRepository extends JpaRepository<DailyQuestionAnswer, Long> {

    @Query("SELECT dqa FROM DailyQuestionAnswer dqa WHERE dqa.dailyQuestion.id = :dailyQuestionId AND dqa.studentProfile.user.id = :userId")
    Optional<DailyQuestionAnswer> findByDailyQuestionIdAndUserId(@Param("dailyQuestionId") Long dailyQuestionId, @Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(dqa) > 0 THEN true ELSE false END FROM DailyQuestionAnswer dqa WHERE dqa.dailyQuestion.id = :dailyQuestionId AND dqa.studentProfile.user.id = :userId")
    boolean existsByDailyQuestionIdAndUserId(@Param("dailyQuestionId") Long dailyQuestionId, @Param("userId") Long userId);

    @Query("SELECT COUNT(dqa) FROM DailyQuestionAnswer dqa WHERE dqa.dailyQuestion.id = :questionId")
    long countAnswersByQuestionId(@Param("questionId") Long questionId);

    @Query("SELECT dqa.selectedAnswer, COUNT(dqa) " +
            "FROM DailyQuestionAnswer dqa " +
            "WHERE dqa.dailyQuestion.id = :questionId " +
            "GROUP BY dqa.selectedAnswer")
    List<Object[]> getAnswerDistribution(@Param("questionId") Long questionId);

    @Query("SELECT dqa FROM DailyQuestionAnswer dqa WHERE dqa.dailyQuestion.id = :questionId")
    List<DailyQuestionAnswer> findByDailyQuestionId(@Param("questionId") Long questionId);
}
