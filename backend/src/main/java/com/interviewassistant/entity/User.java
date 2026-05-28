package com.interviewassistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 用户实体 — 对应数据库的 users 表。
 *
 * 设计思路：
 * 一个用户可以有多个聊天记录和简历，所以用 @OneToMany 关联。
 * 密码字段绝对不会返回给前端，所以加了 @JsonProperty(access = READ_ONLY)
 * 实际上我们通过 DTO 层来控制返回内容，这里只是双重保险。
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 100)
    private String email;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 在保存到数据库之前自动设置创建时间和更新时间。
     * @PrePersist 是 JPA 提供的生命周期回调，
     * 在 INSERT 之前自动执行，不需要手动调用。
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
