package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AuditLogItemDto(
        Long id,
        String action,
        Long operatorId,
        String operatorRole,
        Long targetGameId,
        String targetAppId,
        boolean success,
        String reason,
        String requestUri,
        String snapshotType,
        Map<String, Object> beforeSnapshot,
        Map<String, Object> afterSnapshot,
        List<AuditFieldDiffDto> fieldDiffs,
        LocalDateTime createdAt
) {
}
