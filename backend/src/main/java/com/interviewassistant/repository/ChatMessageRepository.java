package com.interviewassistant.repository;

import com.interviewassistant.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    long countByUserIdAndRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long userId, String role, LocalDateTime startTime, LocalDateTime endTime);

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    List<ChatMessage> findByUserIdOrderByCreatedAtAsc(Long userId);
}
