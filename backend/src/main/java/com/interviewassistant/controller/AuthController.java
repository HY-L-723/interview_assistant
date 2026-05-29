package com.interviewassistant.controller;

import com.interviewassistant.common.Result;
import com.interviewassistant.dto.LoginRequest;
import com.interviewassistant.dto.LoginResponse;
import com.interviewassistant.dto.RegisterRequest;
import com.interviewassistant.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Result<String>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("收到注册请求: username={}", request.getUsername());
        String message = authService.register(request);
        return ResponseEntity.ok(Result.success(message));
    }

    @PostMapping("/login")
    public ResponseEntity<Result<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("收到登录请求: username={}", request.getUsername());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(Result.success(response));
    }
}
