package com.nexus.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "developer_game_metric_daily", indexes = {
        @Index(name = "idx_dev_metric_daily_developer_date", columnList = "developer_id, metric_date"),
        @Index(name = "idx_dev_metric_daily_app_date", columnList = "app_id, metric_date")
})
public class DeveloperGameMetricDaily {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "developer_id", nullable = false)
    private Long developerId;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "app_id", nullable = false, length = 64)
    private String appId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(nullable = false)
    private Integer installs = 0;

    @Column(nullable = false)
    private Integer launches = 0;

    @Column(name = "active_users", nullable = false)
    private Integer activeUsers = 0;

    @Column(name = "install_failures", nullable = false)
    private Integer installFailures = 0;

    @Column(name = "launch_failures", nullable = false)
    private Integer launchFailures = 0;

    @Column(name = "avg_session_minutes", nullable = false, precision = 10, scale = 2)
    private BigDecimal avgSessionMinutes = BigDecimal.ZERO;

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
