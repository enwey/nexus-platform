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
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "launch_ad")
public class LaunchAd {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "image_url", nullable = false, length = 1024)
    private String imageUrl;

    @Column(name = "target_url", nullable = false, length = 1024)
    private String targetUrl;

    @Column(name = "image_version", nullable = false, length = 64)
    private String imageVersion;

    @Column(name = "display_seconds", nullable = false)
    private Integer displaySeconds = 4;

    @Column(name = "sponsor_zh_cn", length = 256)
    private String sponsorZhCn;

    @Column(name = "sponsor_zh_tw", length = 256)
    private String sponsorZhTw;

    @Column(name = "sponsor_en", length = 256)
    private String sponsorEn;

    @Column(name = "title_zh_cn", length = 256)
    private String titleZhCn;

    @Column(name = "title_zh_tw", length = 256)
    private String titleZhTw;

    @Column(name = "title_en", length = 256)
    private String titleEn;

    @Column(name = "description_zh_cn", columnDefinition = "TEXT")
    private String descriptionZhCn;

    @Column(name = "description_zh_tw", columnDefinition = "TEXT")
    private String descriptionZhTw;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(name = "cta_zh_cn", length = 128)
    private String ctaZhCn;

    @Column(name = "cta_zh_tw", length = 128)
    private String ctaZhTw;

    @Column(name = "cta_en", length = 128)
    private String ctaEn;

    @Column(name = "footer_zh_cn", length = 256)
    private String footerZhCn;

    @Column(name = "footer_zh_tw", length = 256)
    private String footerZhTw;

    @Column(name = "footer_en", length = 256)
    private String footerEn;

    @Column(nullable = false, length = 32)
    private String status = "DRAFT";

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (code == null || code.isBlank()) {
            code = "launch_ad_" + UUID.randomUUID();
        }
        if (status == null || status.isBlank()) {
            status = "DRAFT";
        }
        if (displaySeconds == null || displaySeconds < 1) {
            displaySeconds = 4;
        }
        if (imageVersion == null || imageVersion.isBlank()) {
            imageVersion = String.valueOf(System.currentTimeMillis());
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (displaySeconds == null || displaySeconds < 1) {
            displaySeconds = 4;
        }
        updatedAt = LocalDateTime.now();
    }
}
