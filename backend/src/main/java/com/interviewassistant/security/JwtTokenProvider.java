package com.interviewassistant.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 令牌工具 —— 负责三件事：生成令牌、解析令牌、验证令牌。
 *
 * JWT 是什么？
 * 可以理解成一张"电子身份证"。用户登录成功后，服务器发给他一张签过名的身份证。
 * 之后每次请求，用户带着这张证就行，服务器验签名就能确认身份，
 * 不需要每次查数据库。这就是"无状态认证"。
 *
 * JWT 结构（三部分，用 . 分隔）：
 * Header.Payload.Signature
 * - Header：  算法类型（HS256）
 * - Payload：  存放的数据（用户ID、用户名、过期时间）
 * - Signature：签名 = 用密钥对 (Header + Payload) 的哈希值
 *   如果有人篡改 Payload，签名就对不上了，服务器直接拒绝。
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expiration;

    /**
     * 从配置文件注入 JWT 密钥和过期时间。
     * 密钥会被转换成 HMAC-SHA256 算法所需的格式。
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 生成 JWT 令牌。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 从令牌中提取用户 ID。
     * 如果令牌无效或过期，会抛出异常，由全局异常处理器统一处理。
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 验证令牌是否有效。
     *
     * @return true=有效, false=无效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
