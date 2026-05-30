package com.interviewassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试详情响应 DTO（含全部 Q&A）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewDetailResponse {

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
    private List<QuestionResponse> questions;
}
