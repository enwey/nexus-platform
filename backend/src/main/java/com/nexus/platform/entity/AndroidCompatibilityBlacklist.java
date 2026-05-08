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
@Table(name = "android_compatibility_blacklist")
public class AndroidCompatibilityBlacklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "target_type", nullable = false, length = 32)
    private String targetType;

    @Column(name = "target_value", nullable = false, length = 128)
    private String targetValue;

    @Column(name = "min_app_version", length = 32)
    private String minAppVersion;

    @Column(name = "max_app_version", length = 32)
    private String maxAppVersion;

    @Column(name = "min_sdk_int")
    private Integer minSdkInt;

    @Column(name = "max_sdk_int")
    private Integer maxSdkInt;

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
            status = "ACTIVE";
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
