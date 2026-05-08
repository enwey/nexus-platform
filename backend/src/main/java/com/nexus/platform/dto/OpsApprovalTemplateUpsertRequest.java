package com.nexus.platform.dto;

public record OpsApprovalTemplateUpsertRequest(
        String templateCode,
        String templateName,
        String bizType,
        String approvalMode,
        String reviewerRole,
        String stepConfig,
        String status,
        String note
) {
}
