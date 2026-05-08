package com.nexus.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "developer_governance_record")
public class DeveloperGovernanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "developer_id", nullable = false)
    private Long developerId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "action_type", nullable = false, length = 64)
    private String actionType;

    @Column(length = 256)
    private String reason;

    @Column(name = "before_snapshot_json", columnDefinition = "TEXT")
    private String beforeSnapshotJson;

    @Column(name = "after_snapshot_json", columnDefinition = "TEXT")
    private String afterSnapshotJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
