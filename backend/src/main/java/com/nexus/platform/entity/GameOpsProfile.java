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
@Table(name = "game_ops_profile")
public class GameOpsProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false, unique = true)
    private Long gameId;

    @Column(name = "studio_name", length = 128)
    private String studioName;

    @Column(name = "player_count_text", length = 128)
    private String playerCountText;

    @Column(name = "runtime_banner_url", length = 512)
    private String runtimeBannerUrl;

    @Column(name = "runtime_logo_url", length = 512)
    private String runtimeLogoUrl;

    @Column(name = "share_title", length = 128)
    private String shareTitle;

    @Column(name = "share_subtitle", length = 256)
    private String shareSubtitle;

    @Column(name = "share_image_url", length = 512)
    private String shareImageUrl;

    @Column(name = "discover_card_cover_url", length = 512)
    private String discoverCardCoverUrl;

    @Column(name = "discover_card_logo_url", length = 512)
    private String discoverCardLogoUrl;

    @Column(name = "marketing_tagline", length = 160)
    private String marketingTagline;

    @Column(name = "marketing_summary", length = 500)
    private String marketingSummary;

    @Column(name = "feature_highlights_json")
    private String featureHighlightsJson;

    @Column(name = "target_audience", length = 120)
    private String targetAudience;

    @Column(name = "support_email", length = 160)
    private String supportEmail;

    @Column(name = "support_url", length = 512)
    private String supportUrl;

    @Column(name = "community_url", length = 512)
    private String communityUrl;

    @Column(name = "compliance_note", length = 500)
    private String complianceNote;

    @Column(name = "operations_status", nullable = false, length = 32)
    private String operationsStatus = "DRAFT";

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (operationsStatus == null || operationsStatus.isBlank()) {
            operationsStatus = "DRAFT";
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
