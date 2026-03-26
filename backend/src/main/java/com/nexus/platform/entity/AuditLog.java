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
@Table(name = "audit_logs")
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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

