package com.interviewassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交回答请求 DTO。
 */
@Data
public class AnswerRequest {

    /** 面试会话 ID */
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    /** 用户回答内容 */
    @NotBlank(message = "回答内容不能为空")
    private String answer;
}
