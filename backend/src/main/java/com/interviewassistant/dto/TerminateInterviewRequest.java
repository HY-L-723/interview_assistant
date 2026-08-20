package com.interviewassistant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 终止模拟面试请求。 */
@Data
public class TerminateInterviewRequest {

    @NotNull(message = "会话ID不能为空")
    private Long sessionId;
}
