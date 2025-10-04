package com.beratcan.app.repository;

import com.beratcan.app.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTestId(Long testId);

    List<Question> findByTestIdOrderBySequenceNumberAsc(Long testId);

    boolean existsByTestIdAndSequenceNumber(Long testId, Integer sequenceNumber);

    @Query("SELECT MAX(q.sequenceNumber) FROM Question q WHERE q.test.id = :testId")
    Integer findMaxSequenceNumberByTestId(@Param("testId") Long testId);
}
