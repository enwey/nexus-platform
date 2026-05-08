package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record DeveloperCertificationReviewRecordDto(
        Long id,
        Long developerId,
        Long profileId,
        Long operatorId,
        String actionType,
        String beforeStatus,
        String afterStatus,
        String reason,
        String snapshotJson,
        LocalDateTime createdAt
) {
}
