package com.interviewassistant.service;

public interface AIService {

    /**
     * 调用 DeepSeek API 发送消息并获取回复。
     *
     * @param userMessage 用户消息
     * @return AI 回复内容
     */
    String chat(String userMessage);
}
