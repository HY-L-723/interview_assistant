package com.interviewassistant.service.impl;

import com.interviewassistant.entity.ChatMessage;
import com.interviewassistant.entity.Conversation;
import com.interviewassistant.entity.User;
import com.interviewassistant.repository.ChatMessageRepository;
import com.interviewassistant.repository.ConversationRepository;
import com.interviewassistant.repository.UserRepository;
import com.interviewassistant.service.AIService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Optional;

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
    private ConversationRepository conversationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AIService aiService;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    void sendMessageStreamRejectsWhenDailyLimitReached() {
        Long userId = 1L;
        Long conversationId = 10L;
        User user = User.builder().id(userId).username("alice").build();
        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .user(user)
                .title("新对话")
                .build();
        SseEmitter emitter = new SseEmitter();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(chatMessageRepository.countByUserIdAndRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("user"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(50L);

        chatService.sendMessageStream(userId, conversationId, "再问一个问题", null, emitter);

        verify(aiService, never()).chatStream(any(), any());
        verify(chatMessageRepository, never()).save(any());
    }
}
