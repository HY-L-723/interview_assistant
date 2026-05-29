package com.interviewassistant.service;

import reactor.core.publisher.Flux;

public interface AIService {

    String chat(String userMessage);

    String chat(String userMessage, String model);

    Flux<String> chatStream(String userMessage, String model);
}
