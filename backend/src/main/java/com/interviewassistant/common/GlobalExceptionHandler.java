package com.interviewassistant.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * 设计思路：
 * 不用在每个 Controller 里写 try-catch，所有异常统一在这里处理。
 * Spring 会自动把异常路由到对应的方法。
 *
 * 就好比整个系统有个"总客服中心"，无论哪个部门出了问题，
 * 都汇总到这里统一回复，格式一致。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验失败（如 @NotBlank 校验不通过）。
     * 把每个字段的错误信息拼接起来返回。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(Result.error(400, message));
    }

    /**
     * 处理业务逻辑异常（如"用户名已存在"、"密码错误"）。
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Void>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest()
                .body(Result.error(400, ex.getMessage()));
    }

    /**
     * 兜底处理：捕获以上两种都没覆盖的异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "服务器内部错误"));
    }
}
