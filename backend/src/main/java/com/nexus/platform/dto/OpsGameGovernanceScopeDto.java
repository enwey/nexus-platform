package com.nexus.platform.dto;

import java.util.List;

public record OpsGameGovernanceScopeDto(
        String channelMode,
        List<String> channels,
        String regionMode,
        List<String> regions,
        String versionMode,
        String versionMin,
        String versionMax,
        List<String> blockedVersions,
        String governanceNote
) {
}
