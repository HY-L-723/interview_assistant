package com.interviewassistant.service.impl;

import com.interviewassistant.common.BusinessException;
import com.interviewassistant.common.RateLimitExceededException;
import com.interviewassistant.dto.ChatResponse;
import com.interviewassistant.entity.ChatMessage;
import com.interviewassistant.entity.User;
import com.interviewassistant.repository.ChatMessageRepository;
import com.interviewassistant.repository.UserRepository;
import com.interviewassistant.service.AIService;
import com.interviewassistant.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    private static final int DAILY_LIMIT = 50;
    private static final String USER_ROLE = "user";
    private static final String ASSISTANT_ROLE = "assistant";

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
                .orElseThrow(() -> new BusinessException("用户不存在"));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);
        long todayCount = chatMessageRepository.countByUserIdAndRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                userId, USER_ROLE, startOfDay, startOfNextDay);
        if (todayCount >= DAILY_LIMIT) {
            log.warn("用户 {} 触发限流, 今日已提问 {} 次", userId, todayCount);
            throw new RateLimitExceededException("今日提问次数已达上限（" + DAILY_LIMIT + "次），请明天再试");
        }
        log.info("用户 {} 发送消息, 今日第 {} 次提问", userId, todayCount + 1);

        ChatMessage userMessage = chatMessageRepository.save(
                ChatMessage.builder()
                        .user(user)
                        .role(USER_ROLE)
                        .content(message)
                        .build());

        String aiReply = aiService.chat(message);

        ChatMessage assistantMessage = chatMessageRepository.save(
                ChatMessage.builder()
                        .user(user)
                        .role(ASSISTANT_ROLE)
                        .content(aiReply)
                        .build());

        log.info("用户 {} 消息处理完成, userMsgId={}, assistantMsgId={}",
                userId, userMessage.getId(), assistantMessage.getId());

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
