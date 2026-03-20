package com.nexus.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 游戏实体类
 */
@Data
@Entity
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String appId;

    @Column(nullable = false)
    private String name;

    private String description;
    private String iconUrl;

    @Column(name = "download_url")
    private String downloadUrl;

    private String version;
    private String md5;

    @Enumerated(EnumType.STRING)
    private GameStatus status;

    @Column(name = "developer_id")
    private Long developerId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 创建前回调，设置创建时间和更新时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * 更新前回调，设置更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 游戏状态枚举
     */
    public enum GameStatus {
        DRAFT,
        PENDING,
        APPROVED,
        REJECTED
    }
}
