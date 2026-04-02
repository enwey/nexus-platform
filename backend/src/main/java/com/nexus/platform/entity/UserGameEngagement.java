package com.nexus.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(
        name = "user_game_engagement",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_game_engagement_user_app", columnNames = {"user_id", "app_id"})
        },
        indexes = {
                @Index(name = "idx_user_game_engagement_user_last_played", columnList = "user_id,last_played_at"),
                @Index(name = "idx_user_game_engagement_user_favorite", columnList = "user_id,is_favorite,favorite_at")
        }
)
public class UserGameEngagement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "play_count", nullable = false)
    private Long playCount = 0L;

    @Column(name = "last_played_at")
    private LocalDateTime lastPlayedAt;

    @Column(name = "is_favorite", nullable = false)
    private Boolean favorite = false;

    @Column(name = "favorite_at")
    private LocalDateTime favoriteAt;

    @Column(name = "share_count", nullable = false)
    private Long shareCount = 0L;

    @Column(name = "last_shared_at")
    private LocalDateTime lastSharedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (playCount == null) {
            playCount = 0L;
        }
        if (favorite == null) {
            favorite = false;
        }
        if (shareCount == null) {
            shareCount = 0L;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
