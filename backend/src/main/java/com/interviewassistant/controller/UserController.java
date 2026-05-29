package com.interviewassistant.controller;

import com.interviewassistant.common.Result;
import com.interviewassistant.dto.UpdateProfileRequest;
import com.interviewassistant.dto.UserProfileResponse;
import com.interviewassistant.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<Result<UserProfileResponse>> getProfile(@AuthenticationPrincipal Long userId) {
        log.info("获取用户信息: userId={}", userId);
        UserProfileResponse profile = userService.getProfile(userId);
        return ResponseEntity.ok(Result.success(profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<Result<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody UpdateProfileRequest request) {
        log.info("更新用户信息: userId={}", userId);
        UserProfileResponse profile = userService.updateProfile(userId, request);
        return ResponseEntity.ok(Result.success(profile));
    }
}
