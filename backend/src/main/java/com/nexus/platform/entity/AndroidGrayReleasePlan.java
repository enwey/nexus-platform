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
@Table(name = "android_gray_release_plan")
public class AndroidGrayReleasePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_code", nullable = false, length = 64)
    private String planCode;

    @Column(name = "plan_name", nullable = false, length = 128)
    private String planName;

    @Column(nullable = false, length = 32)
    private String status = "DRAFT";

    @Column(name = "channel_code", nullable = false, length = 32)
    private String channelCode;

    @Column(name = "min_app_version", length = 32)
    private String minAppVersion;

    @Column(name = "max_app_version", length = 32)
    private String maxAppVersion;

    @Column(name = "overall_traffic_percentage", nullable = false)
    private Integer overallTrafficPercentage = 10;

    @Column(name = "new_user_percentage", nullable = false)
    private Integer newUserPercentage = 50;

    @Column(name = "returning_user_percentage", nullable = false)
    private Integer returningUserPercentage = 50;

    @Column(name = "whitelist_percentage", nullable = false)
    private Integer whitelistPercentage = 0;

    @Column(name = "region_code", length = 32)
    private String regionCode;

    @Column(name = "device_tier", length = 32)
    private String deviceTier;

    @Column(name = "fallback_policy", nullable = false, length = 32)
    private String fallbackPolicy = "ROLLBACK";

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
        if (fallbackPolicy == null || fallbackPolicy.isBlank()) {
            fallbackPolicy = "ROLLBACK";
        }
        if (overallTrafficPercentage == null || overallTrafficPercentage < 1) {
            overallTrafficPercentage = 10;
        }
        if (newUserPercentage == null || newUserPercentage < 0) {
            newUserPercentage = 50;
        }
        if (returningUserPercentage == null || returningUserPercentage < 0) {
            returningUserPercentage = 50;
        }
        if (whitelistPercentage == null || whitelistPercentage < 0) {
            whitelistPercentage = 0;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
