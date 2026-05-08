package com.nexus.platform.dto;

public record OpsSensitivePolicyUpsertRequest(
        String policyCode,
        String policyName,
        String riskLevel,
        Boolean confirmRequired,
        Boolean auditRequired,
        String scopeType,
        String targetActions,
        String status,
        String note
) {
}
