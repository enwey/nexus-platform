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
@Table(name = "android_ab_experiment")
public class AndroidAbExperiment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "experiment_key", nullable = false, length = 64)
    private String experimentKey;

    @Column(name = "experiment_name", nullable = false, length = 128)
    private String experimentName;

    @Column(nullable = false, length = 32)
    private String status = "DRAFT";

    @Column(name = "layer_key", nullable = false, length = 64)
    private String layerKey;

    @Column(length = 128)
    private String owner;

    @Column(name = "channel_code", length = 32)
    private String channelCode;

    @Column(name = "min_app_version", length = 32)
    private String minAppVersion;

    @Column(name = "max_app_version", length = 32)
    private String maxAppVersion;

    @Column(name = "traffic_percentage", nullable = false)
    private Integer trafficPercentage = 10;

    @Column(name = "variant_a_name", nullable = false, length = 64)
    private String variantAName;

    @Column(name = "variant_a_percentage", nullable = false)
    private Integer variantAPercentage = 50;

    @Column(name = "variant_b_name", nullable = false, length = 64)
    private String variantBName;

    @Column(name = "variant_b_percentage", nullable = false)
    private Integer variantBPercentage = 50;

    @Column(length = 256)
    private String hypothesis;

    @Column(name = "success_metric", length = 128)
    private String successMetric;

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
        if (trafficPercentage == null || trafficPercentage < 1) {
            trafficPercentage = 10;
        }
        if (variantAPercentage == null || variantAPercentage < 0) {
            variantAPercentage = 50;
        }
        if (variantBPercentage == null || variantBPercentage < 0) {
            variantBPercentage = 50;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
