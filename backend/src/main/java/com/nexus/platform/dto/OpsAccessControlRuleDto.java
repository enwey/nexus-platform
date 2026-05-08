package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsAccessControlRuleDto(
        Long id,
        String ruleType,
        String targetValue,
        String status,
        String riskLevel,
        String note,
        LocalDateTime expiresAt,
        String updatedBy,
        LocalDateTime updatedAt
) {
}
