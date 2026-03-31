package com.nexus.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "game_versions", indexes = {
        @Index(name = "idx_game_versions_game_created_at", columnList = "game_id, created_at"),
        @Index(name = "idx_game_versions_status_created_at", columnList = "status, created_at")
})
public class GameVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "version_name", nullable = false, length = 64)
    private String versionName;

    @Column(name = "entry_file", nullable = false, length = 255)
    private String entryFile;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "download_url", length = 255)
    private String downloadUrl;

    @Column(name = "md5", length = 64)
    private String md5;

    @Column(name = "submit_note", length = 256)
    private String submitNote;

    @Column(name = "audit_reason", length = 256)
    private String auditReason;

    @Column(name = "is_forced_update", nullable = false)
    private Boolean forcedUpdate = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private VersionStatus status;

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

    public enum VersionStatus {
        PROCESSING,
        DRAFT,
        SUBMITTED,
        APPROVED,
        REJECTED
    }
}
