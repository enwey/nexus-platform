package com.nexus.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

@Data
@Entity
@Table(name = "games", indexes = {
        @Index(name = "idx_games_status_created_at", columnList = "status, created_at"),
        @Index(name = "idx_games_developer_created_at", columnList = "developer_id, created_at")
})
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String appId;

    @Column(nullable = false)
    private String name;

    private String description;
    private String iconUrl;

    @Column(name = "download_url")
    private String downloadUrl;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "source_storage_key")
    private String sourceStorageKey;

    @Column(name = "upload_file_name", length = 128)
    private String uploadFileName;

    @Column(name = "upload_file_size_bytes")
    private Long uploadFileSizeBytes;

    @Column(name = "upload_processing_failure_reason", length = 256)
    private String uploadProcessingFailureReason;

    @Column(name = "upload_processing_retry_count", nullable = false)
    private Integer uploadProcessingRetryCount = 0;

    @Column(name = "upload_processing_started_at")
    private LocalDateTime uploadProcessingStartedAt;

    @Column(name = "upload_processing_finished_at")
    private LocalDateTime uploadProcessingFinishedAt;

    private String version;
    private String md5;

    @Column(name = "source_md5", length = 64)
    private String sourceMd5;

    @Column(name = "package_format", length = 64)
    private String packageFormat;

    @Column(name = "package_key_ciphertext", length = 512)
    private String packageKeyCiphertext;

    @Column(name = "package_key_nonce", length = 128)
    private String packageKeyNonce;

    @Column(length = 32)
    private String category;

    @Column(name = "tags_json")
    private String tagsJson;

    @Column(name = "requires_online", nullable = false)
    private Boolean requiresOnline = false;

    @Column(name = "visibility_status", nullable = false, length = 32)
    private String visibilityStatus = "VISIBLE";

    @Column(name = "visibility_reason", length = 256)
    private String visibilityReason;

    @Column(name = "visibility_until")
    private LocalDateTime visibilityUntil;

    @Column(name = "channel_governance_mode", nullable = false, length = 32)
    private String channelGovernanceMode = "ALL";

    @Column(name = "channel_governance_values", length = 1024)
    private String channelGovernanceValues;

    @Column(name = "region_governance_mode", nullable = false, length = 32)
    private String regionGovernanceMode = "ALL";

    @Column(name = "region_governance_values", length = 2048)
    private String regionGovernanceValues;

    @Column(name = "version_governance_mode", nullable = false, length = 32)
    private String versionGovernanceMode = "ALL";

    @Column(name = "version_min", length = 64)
    private String versionMin;

    @Column(name = "version_max", length = 64)
    private String versionMax;

    @Column(name = "version_blocklist", length = 1024)
    private String versionBlocklist;

    @Column(name = "governance_note", length = 256)
    private String governanceNote;

    @Enumerated(EnumType.STRING)
    private GameStatus status;

    @Column(name = "developer_id")
    private Long developerId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private Map<String, Map<String, String>> locales;

    @PrePersist
    protected void onCreate() {
        if (visibilityStatus == null || visibilityStatus.isBlank()) {
            visibilityStatus = "VISIBLE";
        }
        if (channelGovernanceMode == null || channelGovernanceMode.isBlank()) {
            channelGovernanceMode = "ALL";
        }
        if (regionGovernanceMode == null || regionGovernanceMode.isBlank()) {
            regionGovernanceMode = "ALL";
        }
        if (versionGovernanceMode == null || versionGovernanceMode.isBlank()) {
            versionGovernanceMode = "ALL";
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum GameStatus {
        PROCESSING,
        DRAFT,
        PENDING,
        APPROVED,
        REJECTED
    }
}
