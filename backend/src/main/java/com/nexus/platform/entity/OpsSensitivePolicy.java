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
@Table(name = "ops_sensitive_policy")
public class OpsSensitivePolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_code", nullable = false, length = 64)
    private String policyCode;

    @Column(name = "policy_name", nullable = false, length = 128)
    private String policyName;

    @Column(name = "risk_level", nullable = false, length = 16)
    private String riskLevel = "HIGH";

    @Column(name = "confirm_required", nullable = false)
    private Boolean confirmRequired = true;

    @Column(name = "audit_required", nullable = false)
    private Boolean auditRequired = true;

    @Column(name = "scope_type", nullable = false, length = 32)
    private String scopeType = "ACTION";

    @Column(name = "target_actions", columnDefinition = "TEXT")
    private String targetActions;

    @Column(nullable = false, length = 16)
    private String status = "ENABLED";

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
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
