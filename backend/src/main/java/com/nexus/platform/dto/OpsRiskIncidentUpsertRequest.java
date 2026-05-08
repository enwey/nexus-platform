package com.nexus.platform.dto;

public record OpsRiskIncidentUpsertRequest(
        String incidentType,
        String severity,
        String title,
        String description,
        String targetAppId,
        Long targetGameId,
        String ownerNote,
        String assignee,
        java.time.LocalDateTime handlingDeadline
) {
}
