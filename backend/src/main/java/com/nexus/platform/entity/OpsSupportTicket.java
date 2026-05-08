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
@Table(name = "ops_support_ticket")
public class OpsSupportTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "developer_id", nullable = false)
    private Long developerId;

    @Column(name = "ticket_no", nullable = false, unique = true, length = 32)
    private String ticketNo;

    @Column(name = "ticket_type", nullable = false, length = 32)
    private String ticketType;

    @Column(nullable = false, length = 16)
    private String priority = "NORMAL";

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "ticket_status", nullable = false, length = 32)
    private String ticketStatus = "OPEN";

    @Column(name = "related_app_id", length = 64)
    private String relatedAppId;

    @Column(name = "assignee_admin_id")
    private Long assigneeAdminId;

    @Column(name = "resolution_summary", length = 256)
    private String resolutionSummary;

    @Column(name = "last_reply_at")
    private LocalDateTime lastReplyAt;

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
