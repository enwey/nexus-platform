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
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String email;
    private String phone;

    @Column(name = "account_status", nullable = false, length = 32)
    private String accountStatus = "ACTIVE";

    @Column(name = "governance_tag", length = 64)
    private String governanceTag;

    @Column(name = "ops_note", length = 256)
    private String opsNote;

    @Column(name = "certification_status", nullable = false, length = 32)
    private String certificationStatus = "UNVERIFIED";

    @Column(name = "risk_level", nullable = false, length = 32)
    private String riskLevel = "NORMAL";

    @Column(name = "whitelist_status", nullable = false, length = 32)
    private String whitelistStatus = "STANDARD";

    @Column(name = "violation_count", nullable = false)
    private Integer violationCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (role == null) {
            role = UserRole.PLAYER;
        }
        if (accountStatus == null || accountStatus.isBlank()) {
            accountStatus = "ACTIVE";
        }
        if (certificationStatus == null || certificationStatus.isBlank()) {
            certificationStatus = "UNVERIFIED";
        }
        if (riskLevel == null || riskLevel.isBlank()) {
            riskLevel = "NORMAL";
        }
        if (whitelistStatus == null || whitelistStatus.isBlank()) {
            whitelistStatus = "STANDARD";
        }
        if (violationCount == null || violationCount < 0) {
            violationCount = 0;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum UserRole {
        ADMIN,
        DEVELOPER,
        PLAYER
    }
}
