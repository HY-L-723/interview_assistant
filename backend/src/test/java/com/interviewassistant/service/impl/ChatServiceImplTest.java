package com.interviewassistant.service.impl;

import com.interviewassistant.entity.ChatMessage;
import com.interviewassistant.entity.Conversation;
import com.interviewassistant.entity.User;
import com.interviewassistant.dto.AIChatMessage;
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
import java.util.List;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

        verifyNoInteractions(aiService);
        verify(chatMessageRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void sendMessageStreamIncludesPreviousConversationMessages() {
        Long userId = 1L;
        Long conversationId = 10L;
        User user = User.builder().id(userId).username("alice").build();
        Conversation conversation = Conversation.builder()
                .id(conversationId).user(user).title("技术问答").build();
        ChatMessage previousUser = ChatMessage.builder()
                .user(user).conversation(conversation).role("user").content("Java 的 JVM 是什么？").build();
        ChatMessage previousAssistant = ChatMessage.builder()
                .user(user).conversation(conversation).role("assistant").content("JVM 是 Java 虚拟机。").build();
        SseEmitter emitter = new SseEmitter();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(chatMessageRepository.countByUserIdAndRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("user"), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            saved.setId(3L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });
        when(chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(
                eq(conversationId), any())).thenReturn(List.of(previousAssistant, previousUser));
        when(aiService.chatStream(any(List.class), eq(null))).thenReturn(Flux.never());

        chatService.sendMessageStream(userId, conversationId, "那它如何管理内存？", null, emitter);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AIChatMessage>> contextCaptor = ArgumentCaptor.forClass(List.class);
        verify(aiService).chatStream(contextCaptor.capture(), eq(null));
        List<AIChatMessage> context = contextCaptor.getValue();
        assertEquals(3, context.size());
        assertEquals("Java 的 JVM 是什么？", context.get(0).content());
        assertEquals("JVM 是 Java 虚拟机。", context.get(1).content());
        assertEquals("那它如何管理内存？", context.get(2).content());
    }
}
