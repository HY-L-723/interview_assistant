package com.interviewassistant.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一 API 返回格式。
 *
 * 设计思路：
 * 所有接口返回相同结构 { code, message, data }，前端只需写一套解析逻辑。
 * 类比：就像快递包裹，无论里面是什么商品，外包装格式都一样，
 * 这样快递员（前端）只需要一种处理流程。
 */
@Data
@AllArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}
