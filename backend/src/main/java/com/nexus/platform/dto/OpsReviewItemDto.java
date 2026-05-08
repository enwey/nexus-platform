package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsReviewItemDto(
        Long id,
        Long gameId,
        String appId,
        String name,
        Long developerId,
        String category,
        String gameStatus,
        String visibilityStatus,
        String visibilityReason,
        LocalDateTime visibilityUntil,
        Long versionId,
        String version,
        String versionStatus,
        String submitNote,
        String auditReason,
        Long assignedReviewerId,
        LocalDateTime assignedAt,
        LocalDateTime dueAt,
        Boolean overdue,
        Long pendingHours,
        String queuePriority,
        String backlogLevel,
        Boolean forcedUpdate,
        Boolean manifestValid,
        String manifestSummary,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt,
        String frontendState
) {
}
