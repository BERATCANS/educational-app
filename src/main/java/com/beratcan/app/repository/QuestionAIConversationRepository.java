package com.beratcan.app.repository;

import com.beratcan.app.entity.QuestionAIConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionAIConversationRepository extends JpaRepository<QuestionAIConversation, Long> {

    Optional<QuestionAIConversation> findByQuestionIdAndUserId(Long questionId, Long userId);
}
