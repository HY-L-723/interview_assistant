package com.interviewassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 登录成功后的返回数据。
 * 不返回密码 —— 即使密码是加密存储的，也不应该通过网络传输。
 * tokenType 写死 "Bearer"，因为这是 JWT 规范的用法。
 */
@Data
@Builder
@AllArgsConstructor
public class LoginResponse {
    private Long userId;
    private String username;
    private String email;
    private String token;
    private String tokenType;     // "Bearer"
}
