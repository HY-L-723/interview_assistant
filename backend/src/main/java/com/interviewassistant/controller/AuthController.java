package com.interviewassistant.controller;

import com.interviewassistant.common.Result;
import com.interviewassistant.dto.LoginRequest;
import com.interviewassistant.dto.LoginResponse;
import com.interviewassistant.dto.RegisterRequest;
import com.interviewassistant.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 —— 处理登录和注册请求。
 *
 * 为什么用 @Valid？
 * 加了这个注解，Spring 在进入方法之前会自动校验 LoginRequest/RegisterRequest 里的
 * @NotBlank、@Size 等规则。校验不通过就直接返回 400，不会进入方法体。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<Result<String>> register(@Valid @RequestBody RegisterRequest request) {
        String message = authService.register(request);
        return ResponseEntity.ok(Result.success(message));
    }

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<Result<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(Result.success(response));
    }
}
