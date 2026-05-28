package com.interviewassistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String education;     // JSON: [{"school":"...","degree":"...","start":"...","end":"..."}]

    @Column(columnDefinition = "TEXT")
    private String skills;        // JSON: ["Java","Spring Boot","Vue"...]

    @Column(columnDefinition = "TEXT")
    private String experience;    // JSON: [{"company":"...","role":"...","desc":"...","start":"...","end":"..."}]

    @Column(columnDefinition = "TEXT")
    private String projects;      // JSON: [{"name":"...","desc":"...","tech":"..."}]

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
