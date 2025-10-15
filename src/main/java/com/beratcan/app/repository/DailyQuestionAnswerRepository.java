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
    
    Optional<DailyQuestionAnswer> findByDailyQuestionIdAndUserId(Long dailyQuestionId, Long userId);
    
    boolean existsByDailyQuestionIdAndUserId(Long dailyQuestionId, Long userId);
    
    @Query("SELECT COUNT(dqa) FROM DailyQuestionAnswer dqa WHERE dqa.dailyQuestion.id = :questionId")
    long countAnswersByQuestionId(@Param("questionId") Long questionId);
    
    @Query("SELECT dqa.selectedAnswer, COUNT(dqa) " +
           "FROM DailyQuestionAnswer dqa " +
           "WHERE dqa.dailyQuestion.id = :questionId " +
           "GROUP BY dqa.selectedAnswer")
    List<Object[]> getAnswerDistribution(@Param("questionId") Long questionId);
}
