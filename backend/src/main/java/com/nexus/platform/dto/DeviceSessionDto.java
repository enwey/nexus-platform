package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record DeviceSessionDto(
        String deviceId,
        String deviceName,
        String model,
        String ip,
        LocalDateTime lastActiveAt,
        boolean current
) {
}
