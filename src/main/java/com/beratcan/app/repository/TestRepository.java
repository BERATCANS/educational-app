package com.beratcan.app.repository;

import com.beratcan.app.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findByTopicId(Long topicId);

    List<Test> findByTopicIdOrderBySequenceNumberAsc(Long topicId);

    boolean existsByTopicIdAndSequenceNumber(Long topicId, Integer sequenceNumber);

    @Query("SELECT MAX(t.sequenceNumber) FROM Test t WHERE t.topic.id = :topicId")
    Integer findMaxSequenceNumberByTopicId(@Param("topicId") Long topicId);

    /**
     * Tests'leri questions ile birlikte getirir (JOIN FETCH)
     * MultipleBagFetchException'ı önlemek için sadece questions fetch edilir
     */
    @Query("SELECT DISTINCT t FROM Test t " +
            "LEFT JOIN FETCH t.questions q " +
            "WHERE t.topic.id IN :topicIds")
    List<Test> findByTopicIdsWithQuestions(@Param("topicIds") List<Long> topicIds);
}
