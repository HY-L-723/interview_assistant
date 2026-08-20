package com.interviewassistant.repository;

import com.interviewassistant.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    long countByUserIdAndRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long userId, String role, LocalDateTime startTime, LocalDateTime endTime);

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /** 按时间倒序读取最近消息，用于构建有界的模型上下文。 */
    List<ChatMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    List<ChatMessage> findByUserIdOrderByCreatedAtAsc(Long userId);
}
