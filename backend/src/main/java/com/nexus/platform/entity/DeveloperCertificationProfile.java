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
@Table(name = "developer_certification_profile")
public class DeveloperCertificationProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "developer_id", nullable = false, unique = true)
    private Long developerId;

    @Column(name = "profile_status", nullable = false, length = 32)
    private String profileStatus = "DRAFT";

    @Column(name = "subject_type", nullable = false, length = 32)
    private String subjectType;

    @Column(name = "subject_name", length = 128)
    private String subjectName;

    @Column(name = "legal_representative", length = 64)
    private String legalRepresentative;

    @Column(name = "contact_name", length = 64)
    private String contactName;

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Column(name = "business_license_no", length = 64)
    private String businessLicenseNo;

    @Column(name = "id_document_no", length = 64)
    private String idDocumentNo;

    @Column(name = "certificate_asset_json", columnDefinition = "TEXT")
    private String certificateAssetJson;

    @Column(length = 256)
    private String note;

    @Column(name = "rejection_reason", length = 256)
    private String rejectionReason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (profileStatus == null || profileStatus.isBlank()) {
            profileStatus = "DRAFT";
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
