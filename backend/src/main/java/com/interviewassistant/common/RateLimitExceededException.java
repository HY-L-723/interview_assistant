package com.interviewassistant.common;

/**
 * 请求限流异常 —— 用户请求频率/次数超过限制时抛出。
 *
 * <p>当前用于每日提问次数限制（每用户每天 50 次），
 * 由 {@link GlobalExceptionHandler#handleRateLimitExceededException} 统一捕获，
 * 返回 HTTP 429 Too Many Requests。</p>
 *
 * <pre>{@code
 * // ChatServiceImpl.sendMessage() 中
 * if (todayCount >= DAILY_LIMIT) {
 *     throw new RateLimitExceededException(
 *         "今日提问次数已达上限（50次），请明天再试");
 * }
 * }</pre>
 *
 * @see GlobalExceptionHandler#handleRateLimitExceededException
 * @see BusinessException
 */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
