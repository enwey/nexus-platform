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
@Table(name = "game_review_appeal")
public class GameReviewAppeal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "developer_id", nullable = false)
    private Long developerId;

    @Column(name = "appeal_status", nullable = false, length = 32)
    private String appealStatus = "SUBMITTED";

    @Column(name = "appeal_reason", nullable = false, length = 512)
    private String appealReason;

    @Column(name = "rejection_snapshot", length = 512)
    private String rejectionSnapshot;

    @Column(name = "review_note", length = 512)
    private String reviewNote;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (appealStatus == null || appealStatus.isBlank()) {
            appealStatus = "SUBMITTED";
        }
        submittedAt = submittedAt == null ? LocalDateTime.now() : submittedAt;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
