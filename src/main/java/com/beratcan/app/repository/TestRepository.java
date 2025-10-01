package com.beratcan.app.repository;

import com.beratcan.app.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findByTopicId(Long topicId);

    List<Test> findByTopicIdOrderBySequenceNumberAsc(Long topicId);

    boolean existsByTopicIdAndSequenceNumber(Long topicId, Integer sequenceNumber);

    Integer findMaxSequenceNumberByTopicId(Long topicId);
}
