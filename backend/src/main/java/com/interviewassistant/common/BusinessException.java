package com.interviewassistant.common;

import lombok.Getter;

/**
 * 业务逻辑异常。
 *
 * <p>Service 层遇到可预期的业务错误（如用户不存在、密码错误、用户名重复等）时抛出，
 * 由 {@link GlobalExceptionHandler#handleBusinessException} 统一捕获，
 * 返回 HTTP 400 + 对应的 code/message。</p>
 *
 * <pre>{@code
 * // 默认 code=400
 * throw new BusinessException("用户名已存在");
 *
 * // 自定义 code
 * throw new BusinessException(402, "账户未激活");
 * }</pre>
 *
 * @see GlobalExceptionHandler#handleBusinessException
 * @see RateLimitExceededException
 */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    /**
     * @param code    业务错误码（对应 HTTP 状态码，默认 400）
     * @param message 错误提示信息（会直接返回给前端）
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 等同于 {@code BusinessException(400, message)}
     */
    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }
}
