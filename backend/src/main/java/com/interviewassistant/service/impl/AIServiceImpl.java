package com.interviewassistant.service.impl;

import com.interviewassistant.ai.DeepSeekRequest;
import com.interviewassistant.ai.DeepSeekResponse;
import com.interviewassistant.common.AIServiceException;
import com.interviewassistant.service.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

@Service
public class AIServiceImpl implements AIService {

    private static final Logger log = LoggerFactory.getLogger(AIServiceImpl.class);

    private final WebClient webClient;
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final String systemPrompt;
    private final Duration timeout;

    public AIServiceImpl(
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.api-url}") String apiUrl,
            @Value("${llm.model}") String model,
            @Value("${llm.timeout}") long timeoutMs,
            @Value("${llm.system-prompt}") String systemPrompt) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String chat(String userMessage) {
        long start = System.currentTimeMillis();

        if (!StringUtils.hasText(apiKey)) {
            throw new AIServiceException("DeepSeek API Key 未配置，请设置环境变量 DEEPSEEK_API_KEY");
        }

        DeepSeekRequest request = new DeepSeekRequest();
        request.setModel(model);
        request.setMessages(List.of(
                new DeepSeekRequest.Message("system", systemPrompt),
                new DeepSeekRequest.Message("user", userMessage)
        ));

        try {
            DeepSeekResponse response = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(DeepSeekResponse.class)
                    .timeout(timeout)
                    .block();

            long elapsed = System.currentTimeMillis() - start;

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()
                    || response.getChoices().get(0).getMessage() == null
                    || !StringUtils.hasText(response.getChoices().get(0).getMessage().getContent())) {
                log.error("DeepSeek 返回空响应, 耗时: {}ms", elapsed);
                throw new AIServiceException("AI 返回了空响应，请稍后重试");
            }

            String content = response.getChoices().get(0).getMessage().getContent();

            DeepSeekResponse.Usage usage = response.getUsage();
            if (usage != null) {
                log.info("AI 响应成功, 耗时: {}ms, tokens: prompt={}, completion={}, total={}",
                        elapsed, usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
            } else {
                log.info("AI 响应成功, 耗时: {}ms", elapsed);
            }

            return content;

        } catch (AIServiceException e) {
            throw e;
        } catch (WebClientResponseException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("DeepSeek API 返回错误, 状态码: {}, 耗时: {}ms, 响应: {}",
                    e.getStatusCode().value(), elapsed, e.getResponseBodyAsString(), e);
            throw new AIServiceException(
                    String.format("AI 服务异常 (HTTP %d)，请稍后重试", e.getStatusCode().value()), e);
        } catch (WebClientRequestException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("DeepSeek API 请求失败 (网络/超时), 耗时: {}ms", elapsed, e);
            throw new AIServiceException("AI 服务连接超时，请稍后重试", e);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("AI 调用未知异常, 耗时: {}ms", elapsed, e);
            throw new AIServiceException("AI 服务暂时不可用，请稍后重试", e);
        }
    }
}
