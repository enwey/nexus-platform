package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsAccessControlRuleUpsertRequest(
        String ruleType,
        String targetValue,
        String status,
        String riskLevel,
        String note,
        LocalDateTime expiresAt
) {
}
