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
@Table(name = "ops_popup_campaign")
public class OpsPopupCampaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "popup_code", nullable = false, length = 64)
    private String popupCode;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, length = 32)
    private String status = "DRAFT";

    @Column(nullable = false, length = 32)
    private String audience = "ALL";

    @Column(name = "trigger_scene", nullable = false, length = 32)
    private String triggerScene = "APP_LAUNCH";

    @Column(name = "landing_url", length = 512)
    private String landingUrl;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "button_text", length = 64)
    private String buttonText;

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(name = "frequency_limit_per_day", nullable = false)
    private Integer frequencyLimitPerDay = 1;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(length = 256)
    private String note;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null || status.isBlank()) {
            status = "DRAFT";
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
