package com.nexus.platform.dto;

import java.util.List;

public record DeveloperVersionPreflightDto(
        Long gameId,
        Long versionId,
        String versionName,
        Boolean ready,
        Boolean duplicatePackageDetected,
        Boolean duplicateVersionNameDetected,
        Boolean manifestValid,
        Boolean reviewQueueAvailable,
        Boolean forceUpdateAllowed,
        String blockingReason,
        List<PreflightItem> items
) {
    public record PreflightItem(
            String key,
            String label,
            Boolean passed,
            Boolean blocking,
            String detail
    ) {
    }
}
