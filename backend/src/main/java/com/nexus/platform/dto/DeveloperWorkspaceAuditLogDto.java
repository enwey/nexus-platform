package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record DeveloperWorkspaceAuditLogDto(
        Long id,
        String action,
        boolean success,
        String reason,
        String requestUri,
        LocalDateTime createdAt
) {
}
