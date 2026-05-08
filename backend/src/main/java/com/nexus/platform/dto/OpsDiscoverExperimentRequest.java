package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsDiscoverExperimentRequest(
        Long id,
        String scopeCode,
        String experimentName,
        Integer trafficPercent,
        String status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String note
) {
}
