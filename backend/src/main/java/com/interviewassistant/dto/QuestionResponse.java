package com.interviewassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 面试题目响应 DTO。
 * 用于面试详情、历史记录等接口的返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

    private Long id;
    private Integer questionNumber;
    private String questionText;
    private String category;
    private String difficulty;
    private String userAnswer;
    private BigDecimal score;
    private String comment;
    private String referenceAnswer;
    private LocalDateTime answeredAt;
    private LocalDateTime createdAt;
}
