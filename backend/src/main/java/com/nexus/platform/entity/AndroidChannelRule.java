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
@Table(name = "android_channel_rule")
public class AndroidChannelRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_code", nullable = false, length = 32)
    private String channelCode;

    @Column(name = "channel_name", nullable = false, length = 64)
    private String channelName;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "minimum_version", length = 32)
    private String minimumVersion;

    @Column(name = "force_update_enabled", nullable = false)
    private boolean forceUpdateEnabled;

    @Column(name = "gray_release_enabled", nullable = false)
    private boolean grayReleaseEnabled;

    @Column(name = "traffic_percentage", nullable = false)
    private Integer trafficPercentage = 100;

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
        if (trafficPercentage == null || trafficPercentage < 0) {
            trafficPercentage = 100;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (trafficPercentage == null || trafficPercentage < 0) {
            trafficPercentage = 100;
        }
        updatedAt = LocalDateTime.now();
    }
}
