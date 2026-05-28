package com.interviewassistant.common;

/**
 * AI 服务调用异常。
 * 与普通业务异常区分开，方便 GlobalExceptionHandler 返回不同的 HTTP 状态码。
 */
public class AIServiceException extends RuntimeException {

    public AIServiceException(String message) {
        super(message);
    }

    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
