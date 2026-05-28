package com.interviewassistant.service;

import com.interviewassistant.dto.ChatResponse;

public interface ChatService {

    ChatResponse sendMessage(Long userId, String message);
}
