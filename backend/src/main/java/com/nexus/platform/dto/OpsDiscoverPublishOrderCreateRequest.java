package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsDiscoverPublishOrderCreateRequest(
        String scopeCode,
        LocalDateTime effectiveAt,
        String reason
) {
}
