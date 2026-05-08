package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsPublishOrderCreateRequest(
        String assetType,
        Long targetId,
        Long relatedGameId,
        String desiredAction,
        String rolloutMode,
        Integer rolloutPercent,
        String rolloutChannel,
        LocalDateTime scheduleAt,
        String executionNote
) {
}
