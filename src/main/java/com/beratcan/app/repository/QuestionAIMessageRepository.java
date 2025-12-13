package com.beratcan.app.repository;

import com.beratcan.app.entity.QuestionAIMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionAIMessageRepository extends JpaRepository<QuestionAIMessage, Long> {

    List<QuestionAIMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
