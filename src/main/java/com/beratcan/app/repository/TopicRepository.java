package com.beratcan.app.repository;

import com.beratcan.app.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findByBookId(Long bookId);

    List<Topic> findByBookIdOrderBySequenceNumberAsc(Long bookId);

    boolean existsByBookIdAndSequenceNumber(Long bookId, Integer sequenceNumber);

    @Query("SELECT MAX(t.sequenceNumber) FROM Topic t WHERE t.book.id = :bookId")
    Integer findMaxSequenceNumberByBookId(@Param("bookId") Long bookId);
}
