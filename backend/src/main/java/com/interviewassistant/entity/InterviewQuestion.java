package com.interviewassistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 面试题目与回答实体。
 *
 * <p>一次面试中的每一道题对应一条记录，包含题目内容、用户回答、
 * AI 评分和评价。与 {@link InterviewSession} 多对一关联。</p>
 *
 * @see InterviewSession
 */
@Entity
@Table(name = "interview_questions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    /** 题目序号，从 1 开始 */
    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    /** 题目内容 */
    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    /** 题目分类，如"基础知识""项目经验""系统设计" */
    @Column(length = 50)
    private String category;

    /** 题目难度：基础 / 进阶 / 综合 */
    @Column(length = 20)
    private String difficulty;

    /** 用户回答内容 */
    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    /** AI 对该题回答的评分（0-100） */
    @Column(precision = 4, scale = 1)
    private java.math.BigDecimal score;

    /** AI 对该题回答的评价 */
    @Column(columnDefinition = "TEXT")
    private String comment;

    /** AI 提供的参考答案要点 */
    @Column(name = "reference_answer", columnDefinition = "TEXT")
    private String referenceAnswer;

    /** 用户回答时间 */
    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
