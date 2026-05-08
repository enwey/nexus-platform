package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record GameReviewAppealDto(
        Long id,
        Long gameId,
        Long versionId,
        Long developerId,
        String appId,
        String gameName,
        String versionName,
        String versionStatus,
        String appealStatus,
        String appealReason,
        String rejectionSnapshot,
        String reviewNote,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        Long reviewedBy,
        LocalDateTime updatedAt
) {
}
