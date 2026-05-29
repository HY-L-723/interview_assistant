package com.interviewassistant.controller;

import com.interviewassistant.common.Result;
import com.interviewassistant.dto.ChatRequest;
import com.interviewassistant.dto.ChatResponse;
import com.interviewassistant.service.ChatService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<Result<ChatResponse>> chat(@AuthenticationPrincipal Long userId,
                                                     @Valid @RequestBody ChatRequest request) {
        log.info("收到聊天请求: userId={}", userId);
        ChatResponse response = chatService.sendMessage(userId, request.getMessage());
        return ResponseEntity.ok(Result.success(response));
    }
}
