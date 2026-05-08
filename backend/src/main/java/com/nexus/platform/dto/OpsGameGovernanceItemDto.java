package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OpsGameGovernanceItemDto(
        Long id,
        String appId,
        Long developerId,
        String name,
        String description,
        String iconUrl,
        String category,
        List<String> tags,
        String version,
        String status,
        String frontendState,
        String visibilityStatus,
        String visibilityReason,
        LocalDateTime visibilityUntil,
        OpsGameGovernanceScopeDto governanceScope,
        String exposureState,
        String impactLevel,
        Integer activeControlCount,
        Integer affectedSurfaceCount,
        String governanceSummary,
        Boolean requiresOnline,
        Integer versionCount,
        String latestVersionStatus,
        String latestAuditReason,
        Boolean latestManifestValid,
        String latestManifestSummary,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
