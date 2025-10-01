package com.beratcan.app.repository;

import com.beratcan.app.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTestId(Long testId);

    List<Question> findByTestIdOrderBySequenceNumberAsc(Long testId);

    boolean existsByTestIdAndSequenceNumber(Long testId, Integer sequenceNumber);

    Integer findMaxSequenceNumberByTestId(Long testId);
}
