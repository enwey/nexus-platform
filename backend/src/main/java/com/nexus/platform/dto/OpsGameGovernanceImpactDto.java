package com.nexus.platform.dto;

import java.util.List;

public record OpsGameGovernanceImpactDto(
        Long gameId,
        String appId,
        String gameName,
        String visibilityStatus,
        String exposureState,
        String impactLevel,
        Integer activeControlCount,
        Integer affectedSurfaceCount,
        Integer affectedChannelCount,
        Integer affectedRegionCount,
        Integer affectedVersionCount,
        Boolean hasVersionFloor,
        Boolean hasVersionCeiling,
        List<String> highlights,
        String summary,
        String governanceNote
) {
}
