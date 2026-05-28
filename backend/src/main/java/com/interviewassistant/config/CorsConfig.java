package com.interviewassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * 跨域配置。
 *
 * 为什么需要跨域？
 * 前端跑在 localhost:5173（Vite 开发服务器），后端跑在 localhost:8080。
 * 浏览器认为不同端口 = 不同来源，默认禁止跨域请求。
 * 这个配置告诉浏览器："来自 5173 的请求是安全的，允许它访问。"
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));   // 开发阶段允许所有来源
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);                // 允许携带认证信息（JWT token）
        config.setMaxAge(3600L);                         // 预检请求缓存1小时

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
