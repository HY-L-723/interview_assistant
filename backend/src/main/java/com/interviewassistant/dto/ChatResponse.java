package com.interviewassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private Long userMessageId;
    private String userMessage;
    private LocalDateTime userMessageTime;

    private Long assistantMessageId;
    private String assistantMessage;
    private LocalDateTime assistantMessageTime;
}
