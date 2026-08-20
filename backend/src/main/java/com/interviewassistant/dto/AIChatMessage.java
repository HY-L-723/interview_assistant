package com.interviewassistant.dto;

/**
 * 与具体大模型供应商无关的会话消息。
 */
public record AIChatMessage(String role, String content) {
}
