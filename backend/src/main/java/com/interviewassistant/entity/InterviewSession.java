package com.interviewassistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 模拟面试会话实体。
 *
 * <p>一次完整的模拟面试对应一条记录，包含面试岗位、状态、
 * 题目数量、总体评价等信息。与 {@link User} 多对一关联。</p>
 *
 * <p>状态流转：WAITING_POSITION → IN_PROGRESS → EVALUATING → COMPLETED，
 * 任意非终态都可以被用户主动 TERMINATED。</p>
 *
 * @see InterviewQuestion
 */
@Entity
@Table(name = "interview_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 面试岗位，例如"Java后端开发工程师" */
    @Column(nullable = false, length = 200)
    private String position;

    /**
     * 面试状态。
     * 取值：WAITING_POSITION / IN_PROGRESS / TERMINATED / EVALUATING / COMPLETED
     */
    @Column(nullable = false, length = 30)
    private String status;

    /** 本次面试生成的题目总数 */
    @Column(name = "total_questions")
    private Integer totalQuestions;

    /** 已作答的题目数 */
    @Column(name = "answered_count")
    private Integer answeredCount;

    /** 综合评分（0-100），面试结束后由 AI 生成 */
    @Column(name = "overall_score")
    private java.math.BigDecimal overallScore;

    /** 总体评价，面试结束后由 AI 生成 */
    @Column(name = "overall_comment", columnDefinition = "TEXT")
    private String overallComment;

    /** 学习建议，面试结束后由 AI 生成 */
    @Column(name = "study_advice", columnDefinition = "TEXT")
    private String studyAdvice;

    /** 面试实际开始时间 */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 面试结束时间 */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
