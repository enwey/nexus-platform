package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record DeveloperRuntimeIssueDto(
        Long id,
        Long gameId,
        String appId,
        String gameName,
        String issueType,
        String severity,
        String issueCode,
        String issueMessage,
        Integer impactedUsers,
        Integer impactedDevices,
        String issueStatus,
        LocalDateTime firstOccurredAt,
        LocalDateTime lastOccurredAt
) {
}
