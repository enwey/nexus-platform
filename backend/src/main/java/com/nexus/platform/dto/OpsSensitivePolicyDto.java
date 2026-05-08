package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsSensitivePolicyDto(
        Long id,
        String policyCode,
        String policyName,
        String riskLevel,
        Boolean confirmRequired,
        Boolean auditRequired,
        String scopeType,
        String targetActions,
        String status,
        String note,
        String updatedBy,
        LocalDateTime updatedAt
) {
}
