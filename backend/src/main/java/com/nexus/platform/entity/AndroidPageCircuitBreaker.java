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
@Table(name = "android_page_circuit_breaker")
public class AndroidPageCircuitBreaker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "page_key", nullable = false, length = 64)
    private String pageKey;

    @Column(name = "page_name", nullable = false, length = 128)
    private String pageName;

    @Column(nullable = false, length = 32)
    private String status = "ENABLED";

    @Column(name = "audience_scope", nullable = false, length = 32)
    private String audienceScope = "ALL";

    @Column(name = "channel_code", length = 32)
    private String channelCode;

    @Column(name = "min_app_version", length = 32)
    private String minAppVersion;

    @Column(name = "max_app_version", length = 32)
    private String maxAppVersion;

    @Column(name = "degrade_mode", nullable = false, length = 32)
    private String degradeMode = "HIDE";

    @Column(nullable = false, length = 256)
    private String reason;

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
            status = "ENABLED";
        }
        if (audienceScope == null || audienceScope.isBlank()) {
            audienceScope = "ALL";
        }
        if (degradeMode == null || degradeMode.isBlank()) {
            degradeMode = "HIDE";
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
