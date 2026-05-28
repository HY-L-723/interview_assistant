package com.interviewassistant.service.impl;

import com.interviewassistant.dto.ChatResponse;
import com.interviewassistant.entity.ChatMessage;
import com.interviewassistant.entity.User;
import com.interviewassistant.repository.ChatMessageRepository;
import com.interviewassistant.repository.UserRepository;
import com.interviewassistant.service.AIService;
import com.interviewassistant.service.ChatService;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final AIService aiService;

    public ChatServiceImpl(ChatMessageRepository chatMessageRepository,
                           UserRepository userRepository,
                           AIService aiService) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
    }

    @Override
    public ChatResponse sendMessage(Long userId, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 先保存用户消息，AI 调用失败时也保留用户提问
        ChatMessage userMessage = chatMessageRepository.save(
                ChatMessage.builder()
                        .user(user)
                        .role("user")
                        .content(message)
                        .build());

        // AI 调用不持有数据库连接
        String aiReply = aiService.chat(message);

        // AI 成功返回后保存助手回复
        ChatMessage assistantMessage = chatMessageRepository.save(
                ChatMessage.builder()
                        .user(user)
                        .role("assistant")
                        .content(aiReply)
                        .build());

        return ChatResponse.builder()
                .userMessageId(userMessage.getId())
                .userMessage(userMessage.getContent())
                .userMessageTime(userMessage.getCreatedAt())
                .assistantMessageId(assistantMessage.getId())
                .assistantMessage(assistantMessage.getContent())
                .assistantMessageTime(assistantMessage.getCreatedAt())
                .build();
    }

}
