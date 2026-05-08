package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsRiskIncidentRecordDto(
        Long id,
        Long incidentId,
        Long operatorId,
        String actionType,
        String fromStatus,
        String toStatus,
        String note,
        LocalDateTime createdAt
) {
}
