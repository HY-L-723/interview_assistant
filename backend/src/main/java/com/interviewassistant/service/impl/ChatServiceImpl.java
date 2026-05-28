package com.interviewassistant.service.impl;

import com.interviewassistant.dto.ChatResponse;
import com.interviewassistant.entity.ChatMessage;
import com.interviewassistant.entity.User;
import com.interviewassistant.repository.ChatMessageRepository;
import com.interviewassistant.repository.UserRepository;
import com.interviewassistant.service.ChatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatServiceImpl(ChatMessageRepository chatMessageRepository,
                           UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ChatResponse sendMessage(Long userId, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        ChatMessage userMessage = ChatMessage.builder()
                .user(user)
                .role("user")
                .content(message)
                .build();
        chatMessageRepository.save(userMessage);

        String aiReply = generateMockResponse(message);

        ChatMessage assistantMessage = ChatMessage.builder()
                .user(user)
                .role("assistant")
                .content(aiReply)
                .build();
        chatMessageRepository.save(assistantMessage);

        return ChatResponse.builder()
                .userMessageId(userMessage.getId())
                .userMessage(userMessage.getContent())
                .userMessageTime(userMessage.getCreatedAt())
                .assistantMessageId(assistantMessage.getId())
                .assistantMessage(assistantMessage.getContent())
                .assistantMessageTime(assistantMessage.getCreatedAt())
                .build();
    }

    private String generateMockResponse(String userMessage) {
        return "这是对「" + userMessage + "」的模拟回复。AI 功能尚未接入，请耐心等待。";
    }
}
