package com.nexus.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_audit_logs_action_created_at", columnList = "action, created_at"),
        @Index(name = "idx_audit_logs_success_created_at", columnList = "success, created_at"),
        @Index(name = "idx_audit_logs_operator_created_at", columnList = "operator_id, created_at"),
        @Index(name = "idx_audit_logs_target_app_created_at", columnList = "target_app_id, created_at")
})
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "operator_role", nullable = false, length = 32)
    private String operatorRole;

    @Column(name = "target_game_id")
    private Long targetGameId;

    @Column(name = "target_app_id", length = 64)
    private String targetAppId;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 256)
    private String reason;

    @Column(name = "request_uri", length = 256)
    private String requestUri;

    @Column(name = "snapshot_type", length = 64)
    private String snapshotType;

    @Column(name = "before_snapshot_json", columnDefinition = "TEXT")
    private String beforeSnapshotJson;

    @Column(name = "after_snapshot_json", columnDefinition = "TEXT")
    private String afterSnapshotJson;

    @Column(name = "diff_json", columnDefinition = "TEXT")
    private String diffJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
