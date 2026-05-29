package com.interviewassistant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewassistant.common.Result;
import com.interviewassistant.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类。
 *
 * 三个核心决策：
 *
 * 1. 密码加密用 BCrypt：
 *    BCrypt 是一种"慢哈希"算法，故意让计算变慢（约0.1秒/次）。
 *    对正常登录没影响，但对暴力破解攻击 —— 攻击者想试100万个密码就要等28小时。
 *    而且每次加密结果不同（自动加随机盐），彩虹表攻击也无效。
 *
 * 2. Session 设为 STATELESS：
 *    传统 Web 应用用服务器 Session 保存登录状态，但我们的 JWT 本身就是"携带状态的令牌"。
 *    服务器不需要记任何东西，每次请求从 token 中解析用户信息即可。
 *    好处：方便水平扩展，加多少台服务器都不影响。
 *
 * 3. 放行登录/注册接口：
 *    用户还没登录呢，当然不能要求他们带 token。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF：JWT 本身就能防跨站请求伪造，不需要额外的 CSRF token
            .csrf(csrf -> csrf.disable())

            // 无状态 Session
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 接口权限配置
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .anyRequest().authenticated()
            )

            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) ->
                        writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                                Result.error(401, "未登录或 token 已过期")))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                                Result.error(403, "权限不足")))
            )

            // 把 JWT 过滤器加到 Spring Security 过滤器链中
            // 放在 UsernamePasswordAuthenticationFilter 之前，确保先解析 JWT
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeErrorResponse(HttpServletResponse response,
                                    int status,
                                    Result<Void> body) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }

    /**
     * 暴露 AuthenticationManager 为 Bean，
     * 虽然当前用不上（我们手动调用 BCrypt 验证密码），
     * 但保留它可以为将来扩展（如 OAuth2 登录）做准备。
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
