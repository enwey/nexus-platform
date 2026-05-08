package com.nexus.platform.dto;

import java.math.BigDecimal;
import java.util.List;

public record DeveloperOperationsDashboardDto(
        Overview overview,
        List<DeveloperMetricTrendPointDto> trend,
        List<GameOpsRow> games,
        List<DeveloperRuntimeIssueDto> runtimeIssues
) {
    public record Overview(
            Integer installs7d,
            Integer launches7d,
            Integer activeUsers7d,
            Integer installFailures7d,
            Integer launchFailures7d,
            BigDecimal avgSessionMinutes7d,
            Integer impactedGames,
            Integer openIssueCount
    ) {
    }

    public record GameOpsRow(
            Long gameId,
            String appId,
            String gameName,
            Integer installs7d,
            Integer launches7d,
            Integer activeUsers7d,
            Integer installFailures7d,
            Integer launchFailures7d,
            BigDecimal avgSessionMinutes7d,
            Integer openIssueCount
    ) {
    }
}
