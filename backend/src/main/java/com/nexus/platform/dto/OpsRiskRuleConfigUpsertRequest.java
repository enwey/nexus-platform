package com.nexus.platform.dto;

public record OpsRiskRuleConfigUpsertRequest(
        String ruleCode,
        String ruleName,
        Boolean enabled,
        String severity,
        Integer thresholdCount,
        Integer windowMinutes,
        String actionType,
        String note
) {
}
