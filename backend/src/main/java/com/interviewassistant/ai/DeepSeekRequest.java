package com.interviewassistant.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DeepSeekRequest {

    private String model;
    private List<Message> messages;
    private Map<String, String> thinking = Map.of("type", "disabled");

    private boolean stream = false;

    @Data
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}
