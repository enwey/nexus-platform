package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DeveloperCertificationProfileDto(
        Long id,
        Long developerId,
        String profileStatus,
        String certificationStatus,
        String subjectType,
        String subjectName,
        String legalRepresentative,
        String contactName,
        String contactPhone,
        String businessLicenseNo,
        String idDocumentNo,
        List<String> certificateAssets,
        String note,
        String rejectionReason,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        LocalDateTime updatedAt
) {
}
