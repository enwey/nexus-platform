package com.nexus.platform.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DeveloperMetricTrendPointDto(
        LocalDate metricDate,
        Integer installs,
        Integer launches,
        Integer activeUsers,
        Integer installFailures,
        Integer launchFailures,
        BigDecimal avgSessionMinutes
) {
}
