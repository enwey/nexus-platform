package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsRiskRuleConfigDto(
        Long id,
        String ruleCode,
        String ruleName,
        Boolean enabled,
        String severity,
        Integer thresholdCount,
        Integer windowMinutes,
        String actionType,
        String note,
        String updatedBy,
        LocalDateTime updatedAt
) {
}
