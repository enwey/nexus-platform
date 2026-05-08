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
@Table(name = "ops_publish_order")
public class OpsPublishOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_type", nullable = false, length = 32)
    private String assetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "asset_code", nullable = false, length = 64)
    private String assetCode;

    @Column(name = "asset_name", nullable = false, length = 128)
    private String assetName;

    @Column(name = "current_status", length = 64)
    private String currentStatus;

    @Column(name = "desired_action", nullable = false, length = 32)
    private String desiredAction;

    @Column(name = "rollout_mode", nullable = false, length = 16)
    private String rolloutMode = "FULL";

    @Column(name = "rollout_percent")
    private Integer rolloutPercent;

    @Column(name = "rollout_channel", length = 64)
    private String rolloutChannel;

    @Column(name = "order_status", nullable = false, length = 32)
    private String orderStatus = "SUBMITTED";

    @Column(name = "schedule_at")
    private LocalDateTime scheduleAt;

    @Column(name = "execution_note", length = 256)
    private String executionNote;

    @Column(name = "reject_reason", length = 256)
    private String rejectReason;

    @Column(name = "execution_result", length = 32)
    private String executionResult = "PENDING";

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(name = "execution_receipt", columnDefinition = "TEXT")
    private String executionReceipt;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "desired_snapshot_json", columnDefinition = "TEXT")
    private String desiredSnapshotJson;

    @Column(name = "execution_before_snapshot_json", columnDefinition = "TEXT")
    private String executionBeforeSnapshotJson;

    @Column(name = "execution_after_snapshot_json", columnDefinition = "TEXT")
    private String executionAfterSnapshotJson;

    @Column(name = "config_diff_json", columnDefinition = "TEXT")
    private String configDiffJson;

    @Column(name = "submitted_by")
    private Long submittedBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "published_by")
    private Long publishedBy;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (orderStatus == null || orderStatus.isBlank()) {
            orderStatus = "SUBMITTED";
        }
        if (rolloutMode == null || rolloutMode.isBlank()) {
            rolloutMode = "FULL";
        }
        if (executionResult == null || executionResult.isBlank()) {
            executionResult = "PENDING";
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
