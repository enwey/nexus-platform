package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OpsDiscoverAnalyticsDto(
        int totalDraftItems,
        int totalLiveItems,
        int totalScheduledItems,
        int totalPausedItems,
        int totalExpiredItems,
        int scheduledPublishOrders,
        int activeExperiments,
        int scheduledExperiments,
        int totalExperimentTraffic,
        LocalDateTime nearestPublishAt,
        List<ScopeMetrics> scopes
) {
    public record ScopeMetrics(
            String scopeCode,
            String scopeName,
            int draftItems,
            int liveItems,
            int scheduledItems,
            int pausedItems,
            int expiredItems,
            int pendingPublishOrders,
            int activeExperiments,
            int scheduledExperiments,
            int experimentTrafficPercent,
            LocalDateTime nextPublishAt
    ) {
    }
}
