package com.nexus.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "game_media_assets")
public class GameMediaAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "asset_group", nullable = false, length = 32)
    private String assetGroup = "LIBRARY";

    @Column(name = "asset_role", nullable = false, length = 32)
    private String assetRole = "SCREENSHOT";

    @Column(name = "media_type", nullable = false, length = 32)
    private String mediaType;

    @Column(name = "asset_status", nullable = false, length = 32)
    private String assetStatus = "DRAFT";

    @Column(length = 80)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 512)
    private String url;

    @Column(name = "action_title", length = 40)
    private String actionTitle;

    @Column(name = "action_url", length = 512)
    private String actionUrl;

    private Integer width;
    private Integer height;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(nullable = false, length = 16)
    private String locale = "zh-CN";

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "is_primary", nullable = false)
    private Boolean primary = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (assetGroup == null || assetGroup.isBlank()) {
            assetGroup = "LIBRARY";
        }
        if (assetRole == null || assetRole.isBlank()) {
            assetRole = "SCREENSHOT";
        }
        if (assetStatus == null || assetStatus.isBlank()) {
            assetStatus = "DRAFT";
        }
        if (locale == null || locale.isBlank()) {
            locale = "zh-CN";
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
        if (primary == null) {
            primary = false;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
