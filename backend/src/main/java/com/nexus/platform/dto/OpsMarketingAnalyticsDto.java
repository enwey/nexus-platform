package com.nexus.platform.dto;

import java.util.List;

public record OpsMarketingAnalyticsDto(
        int totalAssets,
        int liveAssets,
        int scheduledAssets,
        int draftAssets,
        int pausedAssets,
        int archivedAssets,
        int expiredAssets,
        int availableGiftCodes,
        int redeemedGiftCodes,
        List<AssetMetrics> metrics
) {
    public record AssetMetrics(
            String assetType,
            String assetName,
            int total,
            int live,
            int scheduled,
            int draft,
            int paused,
            int archived,
            int expired,
            int stock,
            int redeemed
    ) {
    }
}
