package com.interviewassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 面试会话响应 DTO（列表用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSessionResponse {

    private Long id;
    private String position;
    private String status;
    private Integer totalQuestions;
    private Integer answeredCount;
    private BigDecimal overallScore;
    private String overallComment;
    private String studyAdvice;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
}
