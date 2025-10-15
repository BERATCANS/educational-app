package com.beratcan.app.repository;

import com.beratcan.app.entity.DailyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyQuestionRepository extends JpaRepository<DailyQuestion, Long> {
    
    Optional<DailyQuestion> findByDisplayDate(LocalDate displayDate);
    
    boolean existsByDisplayDate(LocalDate displayDate);
}
