package com.interviewassistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO。
 * @NotBlank 确保 username 和 password 不能为空（包括不能是纯空格）。
 * 这个校验由 Spring Validation 自动完成，不需要手动写 if 判断。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
