package com.interviewassistant.common;

/**
 * AI 服务调用异常 —— 调用 DeepSeek API 失败时抛出。
 *
 * <p>AIServiceImpl 在遇到以下情况时抛出此异常：
 * <ul>
 *   <li>API Key 未配置</li>
 *   <li>API 返回空响应</li>
 *   <li>HTTP 错误（4xx / 5xx）</li>
 *   <li>网络超时 / 连接失败</li>
 *   <li>其他未知调用异常</li>
 * </ul>
 *
 * <p>由 {@link GlobalExceptionHandler#handleAIServiceException} 统一捕获，
 * 返回 HTTP 503 Service Unavailable。</p>
 *
 * <pre>{@code
 * // 仅消息
 * throw new AIServiceException("API Key 未配置");
 *
 * // 消息 + 原始异常（保留堆栈信息，方便排查）
 * throw new AIServiceException("AI 服务连接超时，请稍后重试", e);
 * }</pre>
 *
 * @see GlobalExceptionHandler#handleAIServiceException
 */
public class AIServiceException extends RuntimeException {

    public AIServiceException(String message) {
        super(message);
    }

    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
