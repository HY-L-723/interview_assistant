package com.interviewassistant.repository;

import com.interviewassistant.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 面试题目数据访问层。
 */
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    /** 按会话 ID 查询所有题目，按题号升序 */
    List<InterviewQuestion> findBySessionIdOrderByQuestionNumberAsc(Long sessionId);
}
