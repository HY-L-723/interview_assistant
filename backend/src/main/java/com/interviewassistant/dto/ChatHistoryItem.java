package com.interviewassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChatHistoryItem {
    private Long id;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
