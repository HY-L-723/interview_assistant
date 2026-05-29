package com.interviewassistant.service;

import com.interviewassistant.dto.ChatHistoryItem;
import com.interviewassistant.dto.ConversationDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {

    void sendMessageStream(Long userId, Long conversationId, String message, String model, SseEmitter emitter);

    ConversationDto createConversation(Long userId);

    List<ConversationDto> listConversations(Long userId);

    List<ChatHistoryItem> getConversationMessages(Long userId, Long conversationId);

    void deleteConversation(Long userId, Long conversationId);
}
