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
@Table(name = "verification_code_logs", indexes = {
        @Index(name = "idx_vcode_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_vcode_logs_account", columnList = "account"),
        @Index(name = "idx_vcode_logs_purpose", columnList = "purpose"),
        @Index(name = "idx_vcode_logs_source", columnList = "request_source")
})
public class VerificationCodeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 191)
    private String account;

    @Column(nullable = false, length = 32)
    private String purpose;

    @Column(name = "debug_code", length = 16)
    private String debugCode;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "failure_reason", length = 256)
    private String failureReason;

    @Column(name = "request_source", length = 64)
    private String requestSource;

    @Column(name = "request_scene", length = 64)
    private String requestScene;

    @Column(name = "request_ip", length = 64)
    private String requestIp;

    @Column(name = "request_uri", length = 256)
    private String requestUri;

    @Column(name = "requester_user_id")
    private Long requesterUserId;

    @Column(name = "requester_role", length = 32)
    private String requesterRole;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
