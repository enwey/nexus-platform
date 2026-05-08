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
@Table(name = "developer_certification_review_record")
public class DeveloperCertificationReviewRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "developer_id", nullable = false)
    private Long developerId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "action_type", nullable = false, length = 32)
    private String actionType;

    @Column(name = "before_status", length = 32)
    private String beforeStatus;

    @Column(name = "after_status", nullable = false, length = 32)
    private String afterStatus;

    @Column(length = 256)
    private String reason;

    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
