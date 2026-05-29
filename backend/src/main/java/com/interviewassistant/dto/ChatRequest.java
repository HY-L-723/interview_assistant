package com.interviewassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "消息不能为空")
    private String message;

    @NotNull(message = "对话ID不能为空")
    private Long conversationId;

    private String model;
}
