package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsLoginRiskEventDto(
        Long id,
        String loginId,
        Long userId,
        String clientIp,
        String deviceId,
        String userAgent,
        String eventType,
        String severity,
        String result,
        String reason,
        LocalDateTime createdAt
) {
}
