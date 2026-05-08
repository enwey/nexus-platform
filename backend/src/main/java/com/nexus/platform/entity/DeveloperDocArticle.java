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
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "developer_doc_article", indexes = {
        @Index(name = "idx_dev_doc_article_developer_type", columnList = "developer_id, doc_type, updated_at"),
        @Index(name = "idx_dev_doc_article_developer_status", columnList = "developer_id, article_status, updated_at")
})
public class DeveloperDocArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "developer_id", nullable = false)
    private Long developerId;

    @Column(name = "doc_type", nullable = false, length = 32)
    private String docType;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(length = 64)
    private String category;

    @Column(name = "article_status", nullable = false, length = 32)
    private String articleStatus = "DRAFT";

    @Column(length = 200)
    private String summary;

    @Column(name = "tags_json", length = 512)
    private String tagsJson;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion = 1;

    @Column(name = "latest_change_note", length = 120)
    private String latestChangeNote;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

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
