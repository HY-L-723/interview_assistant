package com.interviewassistant.service.impl;

import com.interviewassistant.dto.LoginRequest;
import com.interviewassistant.dto.LoginResponse;
import com.interviewassistant.dto.RegisterRequest;
import com.interviewassistant.entity.User;
import com.interviewassistant.repository.UserRepository;
import com.interviewassistant.security.JwtTokenProvider;
import com.interviewassistant.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现。
 *
 * 注册流程：
 * 1. 检查用户名是否已存在
 * 2. 用 BCrypt 加密密码（明文密码永不落盘）
 * 3. 保存用户到数据库
 *
 * 登录流程：
 * 1. 根据用户名查找用户
 * 2. 用 BCrypt 比对密码（不是明文比对！）
 * 3. 生成 JWT token 返回
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public String register(RegisterRequest request) {
        // 第一步：检查用户名是否已被占用
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 第二步：加密密码后保存
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))  // BCrypt 加密
                .email(request.getEmail())
                .build();

        userRepository.save(user);
        return "注册成功";
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 第一步：查找用户
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        // 第二步：验证密码
        // BCrypt 的 matches 方法会：从存储的密文中提取盐值 → 用同样盐值加密输入的密码 → 比对结果
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 第三步：生成 JWT token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(token)
                .tokenType("Bearer")
                .build();
    }
}
