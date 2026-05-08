package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsGameVisibilityUpdateRequest(
        String visibilityStatus,
        String reason,
        LocalDateTime visibilityUntil
) {
}
