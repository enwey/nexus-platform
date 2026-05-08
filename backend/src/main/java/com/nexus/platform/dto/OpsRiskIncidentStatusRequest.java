package com.nexus.platform.dto;

public record OpsRiskIncidentStatusRequest(
        String status,
        String ownerNote,
        String assignee,
        String resolutionSummary
) {
}
