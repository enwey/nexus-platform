package com.nexus.platform.dto;

public record OpsDeveloperGovernanceUpdateRequest(
        String accountStatus,
        String certificationStatus,
        String riskLevel,
        String whitelistStatus,
        Integer violationCount,
        String governanceTag,
        String opsNote,
        String reason
) {
}
