package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OpsGameVisibilityBatchUpdateRequest(
        List<Long> gameIds,
        String visibilityStatus,
        String reason,
        LocalDateTime visibilityUntil
) {
}
