package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record DeveloperGovernanceRecordDto(
        Long id,
        Long developerId,
        Long operatorId,
        String actionType,
        String reason,
        String beforeSnapshotJson,
        String afterSnapshotJson,
        LocalDateTime createdAt
) {
}
