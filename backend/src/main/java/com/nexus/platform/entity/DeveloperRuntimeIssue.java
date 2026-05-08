package com.nexus.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "developer_runtime_issue", indexes = {
        @Index(name = "idx_dev_runtime_issue_developer_status", columnList = "developer_id, issue_status, last_occurred_at"),
        @Index(name = "idx_dev_runtime_issue_game_type", columnList = "game_id, issue_type, last_occurred_at")
})
public class DeveloperRuntimeIssue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "developer_id", nullable = false)
    private Long developerId;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "app_id", nullable = false, length = 64)
    private String appId;

    @Column(name = "issue_type", nullable = false, length = 32)
    private String issueType;

    @Column(nullable = false, length = 16)
    private String severity = "MEDIUM";

    @Column(name = "issue_code", nullable = false, length = 64)
    private String issueCode;

    @Column(name = "issue_message", nullable = false, length = 256)
    private String issueMessage;

    @Column(name = "impacted_users", nullable = false)
    private Integer impactedUsers = 0;

    @Column(name = "impacted_devices", nullable = false)
    private Integer impactedDevices = 0;

    @Column(name = "issue_status", nullable = false, length = 32)
    private String issueStatus = "OPEN";

    @Column(name = "first_occurred_at", nullable = false)
    private LocalDateTime firstOccurredAt;

    @Column(name = "last_occurred_at", nullable = false)
    private LocalDateTime lastOccurredAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
