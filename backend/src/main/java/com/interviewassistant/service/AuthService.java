package com.interviewassistant.service;

import com.interviewassistant.dto.LoginRequest;
import com.interviewassistant.dto.LoginResponse;
import com.interviewassistant.dto.RegisterRequest;

public interface AuthService {

    /**
     * 用户注册。
     * 返回注册成功的提示信息。
     */
    String register(RegisterRequest request);

    /**
     * 用户登录。
     * 返回包含 JWT token 的 LoginResponse。
     */
    LoginResponse login(LoginRequest request);
}
