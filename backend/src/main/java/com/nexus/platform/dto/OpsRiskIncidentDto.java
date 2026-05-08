package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsRiskIncidentDto(
        Long id,
        String incidentType,
        String severity,
        String status,
        String title,
        String description,
        String targetAppId,
        Long targetGameId,
        String ownerNote,
        String assignee,
        java.time.LocalDateTime handlingDeadline,
        String resolutionSummary,
        LocalDateTime updatedAt
) {
}
