package com.interviewassistant.repository;

import com.interviewassistant.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 面试会话数据访问层。
 */
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    /** 按用户 ID 查询面试历史，按创建时间倒序 */
    List<InterviewSession> findByUserIdOrderByCreatedAtDesc(Long userId);
}
