package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsApprovalTemplateDto(
        Long id,
        String templateCode,
        String templateName,
        String bizType,
        String approvalMode,
        String reviewerRole,
        String stepConfig,
        String status,
        String note,
        String updatedBy,
        LocalDateTime updatedAt
) {
}
