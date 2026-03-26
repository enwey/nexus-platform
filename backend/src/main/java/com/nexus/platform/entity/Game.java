package com.nexus.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "games", indexes = {
        @Index(name = "idx_games_status_created_at", columnList = "status, created_at"),
        @Index(name = "idx_games_developer_created_at", columnList = "developer_id, created_at")
})
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

    @Column(name = "storage_key")
    private String storageKey;

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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum GameStatus {
        PROCESSING,
        DRAFT,
        PENDING,
        APPROVED,
        REJECTED
    }
}
