package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OpsDiscoverPublishPreviewResponse(
        String scopeCode,
        String scopeName,
        Integer draftCount,
        Integer liveCount,
        Integer addedCount,
        Integer removedCount,
        Integer changedCount,
        LocalDateTime nextScheduledAt,
        List<DiffItem> items,
        List<String> warnings
) {
    public record DiffItem(
            String changeType,
            String appId,
            String title,
            String detail
    ) {
    }
}
