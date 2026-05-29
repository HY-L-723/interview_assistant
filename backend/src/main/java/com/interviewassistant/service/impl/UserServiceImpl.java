package com.interviewassistant.service.impl;

import com.interviewassistant.common.BusinessException;
import com.interviewassistant.dto.UpdateProfileRequest;
import com.interviewassistant.dto.UserProfileResponse;
import com.interviewassistant.entity.User;
import com.interviewassistant.repository.UserRepository;
import com.interviewassistant.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return toResponse(user);
    }

    @Override
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        boolean hasPasswordChange = StringUtils.hasText(request.getNewPassword());
        boolean hasEmailChange = request.getEmail() != null;

        if (hasPasswordChange) {
            if (!StringUtils.hasText(request.getCurrentPassword())) {
                throw new BusinessException("修改密码需要提供当前密码");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new BusinessException("当前密码不正确");
            }
            if (request.getNewPassword().length() < 6 || request.getNewPassword().length() > 100) {
                throw new BusinessException("新密码长度需要在6-100个字符之间");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            log.info("用户 {} 修改了密码", userId);
        }

        if (hasEmailChange) {
            user.setEmail(request.getEmail());
            log.info("用户 {} 修改了邮箱", userId);
        }

        if (hasPasswordChange || hasEmailChange) {
            userRepository.save(user);
        }

        return toResponse(user);
    }

    private UserProfileResponse toResponse(User user) {
        return UserProfileResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
