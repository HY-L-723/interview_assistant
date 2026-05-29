package com.interviewassistant.service.impl;

import com.interviewassistant.common.RateLimitExceededException;
import com.interviewassistant.dto.ChatResponse;
import com.interviewassistant.entity.ChatMessage;
import com.interviewassistant.entity.User;
import com.interviewassistant.repository.ChatMessageRepository;
import com.interviewassistant.repository.UserRepository;
import com.interviewassistant.service.AIService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AIService aiService;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    void sendMessageAllowsFiftiethQuestion() {
        Long userId = 1L;
        User user = User.builder().id(userId).username("alice").build();
        AtomicLong idGenerator = new AtomicLong(100);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(chatMessageRepository.countByUserIdAndRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("user"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(49L);
        when(aiService.chat("请讲一下索引")).thenReturn("索引可以提高查询效率。");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(idGenerator.incrementAndGet());
            message.setCreatedAt(LocalDateTime.now());
            return message;
        });

        ChatResponse response = chatService.sendMessage(userId, "请讲一下索引");

        assertThat(response.getUserMessage()).isEqualTo("请讲一下索引");
        assertThat(response.getAssistantMessage()).isEqualTo("索引可以提高查询效率。");
        verify(aiService).chat("请讲一下索引");
    }

    @Test
    void sendMessageRejectsWhenDailyLimitReached() {
        Long userId = 1L;
        User user = User.builder().id(userId).username("alice").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(chatMessageRepository.countByUserIdAndRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("user"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(50L);

        assertThatThrownBy(() -> chatService.sendMessage(userId, "再问一个问题"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("今日提问次数已达上限");

        verify(aiService, never()).chat(any());
        verify(chatMessageRepository, never()).save(any());
    }
}
