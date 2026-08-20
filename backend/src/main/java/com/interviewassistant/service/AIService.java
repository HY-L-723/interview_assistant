package com.interviewassistant.service;

import com.interviewassistant.dto.AIChatMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AIService {

    String chat(String userMessage);

    String chat(String userMessage, String model);

    Flux<String> chatStream(String userMessage, String model);

    /** 使用完整会话上下文进行流式回答。 */
    Flux<String> chatStream(List<AIChatMessage> messages, String model);
}
