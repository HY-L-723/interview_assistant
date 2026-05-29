package com.interviewassistant.controller;

import com.interviewassistant.common.Result;
import com.interviewassistant.dto.ChatHistoryItem;
import com.interviewassistant.dto.ChatRequest;
import com.interviewassistant.dto.ConversationDto;
import com.interviewassistant.service.ChatService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public SseEmitter chat(@AuthenticationPrincipal Long userId,
                           @Valid @RequestBody ChatRequest request) {
        log.info("收到聊天请求: userId={}, conversationId={}", userId, request.getConversationId());
        SseEmitter emitter = new SseEmitter(120_000L);
        chatService.sendMessageStream(
                userId, request.getConversationId(), request.getMessage(), request.getModel(), emitter);
        return emitter;
    }

    @PostMapping("/conversations")
    public ResponseEntity<Result<ConversationDto>> createConversation(@AuthenticationPrincipal Long userId) {
        log.info("创建对话: userId={}", userId);
        ConversationDto conversation = chatService.createConversation(userId);
        return ResponseEntity.ok(Result.success(conversation));
    }

    @GetMapping("/conversations")
    public ResponseEntity<Result<List<ConversationDto>>> listConversations(@AuthenticationPrincipal Long userId) {
        log.info("获取对话列表: userId={}", userId);
        List<ConversationDto> conversations = chatService.listConversations(userId);
        return ResponseEntity.ok(Result.success(conversations));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<Result<List<ChatHistoryItem>>> getMessages(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        log.info("获取对话消息: userId={}, conversationId={}", userId, id);
        List<ChatHistoryItem> messages = chatService.getConversationMessages(userId, id);
        return ResponseEntity.ok(Result.success(messages));
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Result<Void>> deleteConversation(@AuthenticationPrincipal Long userId,
                                                           @PathVariable Long id) {
        log.info("删除对话: userId={}, conversationId={}", userId, id);
        chatService.deleteConversation(userId, id);
        return ResponseEntity.ok(Result.success());
    }
}
