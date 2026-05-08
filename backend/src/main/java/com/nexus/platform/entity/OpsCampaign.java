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
@Table(name = "ops_campaign")
public class OpsCampaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_code", nullable = false, unique = true, length = 64)
    private String campaignCode;

    @Column(name = "campaign_type", nullable = false, length = 32)
    private String campaignType;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, length = 32)
    private String status = "DRAFT";

    @Column(nullable = false, length = 32)
    private String audience = "ALL";

    @Column(name = "landing_url", length = 512)
    private String landingUrl;

    @Column(name = "banner_url", length = 512)
    private String bannerUrl;

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(length = 256)
    private String note;

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
        if (campaignCode == null || campaignCode.isBlank()) {
            campaignCode = "campaign_" + UUID.randomUUID();
        }
        if (status == null || status.isBlank()) {
            status = "DRAFT";
        }
        if (audience == null || audience.isBlank()) {
            audience = "ALL";
        }
        if (priority == null) {
            priority = 0;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (priority == null) {
            priority = 0;
        }
        updatedAt = LocalDateTime.now();
    }
}
