package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OpsPublishOverviewDto(
        Integer pendingGameReviews,
        Integer blockedGames,
        Integer hiddenGames,
        Integer draftContentItems,
        Integer publishedContentItems,
        Integer pausedContentItems,
        Integer draftLaunchAds,
        Integer activeLaunchAds,
        Integer readyToPublishAssets,
        Integer blockedByRulesAssets,
        Integer scheduledAssets,
        List<PendingPublishItem> pendingItems,
        List<ReleaseGateItem> releaseGateItems,
        List<OpsPublishOrderDto> publishOrders,
        List<RecentPublishAction> recentActions
) {
    public record PendingPublishItem(
            String assetType,
            String assetCode,
            String assetName,
            String status,
            String owner,
            LocalDateTime updatedAt
    ) {
    }

    public record ReleaseGateItem(
            String assetType,
            Long targetId,
            String assetCode,
            String assetName,
            String currentStatus,
            String releaseState,
            String recommendedAction,
            String blockerReason,
            String owner,
            LocalDateTime updatedAt
    ) {
    }

    public record RecentPublishAction(
            String action,
            String targetAppId,
            String reason,
            Boolean success,
            LocalDateTime createdAt
    ) {
    }
}
