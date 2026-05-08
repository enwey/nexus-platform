package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OpsDiscoverConfigResponse(
        HeroConfig hero,
        List<TopBannerConfig> gameTopBanners,
        List<TopBannerConfig> discoverTopBanners,
        List<String> rankedAppIds,
        List<String> newbieAppIds,
        List<String> everyoneAppIds,
        List<CommunityItemConfig> communityItems,
        List<SimpleGameItem> availableGames,
        List<CategoryOption> categoryOptions,
        List<SlotControl> slotControls,
        List<ScopePreview> previews,
        List<PublishOrderSummary> publishOrders,
        List<ExperimentConfig> experiments
) {
    public record HeroConfig(
            String appId,
            String title,
            String subtitle,
            String badgeText,
            String coverUrl
    ) {
    }

    public record TopBannerConfig(
            Long id,
            String appId,
            String title,
            String subtitle,
            String badgeText,
            String coverUrl,
            String status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Integer sortOrder
    ) {
    }

    public record CommunityItemConfig(
            Long id,
            String appId,
            String cardCategory,
            String cardTitle,
            String coverUrl,
            String articleTag,
            String articleTitle,
            String articleBody,
            String actionText,
            String status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Integer sortOrder
    ) {
    }

    public record SimpleGameItem(
            Long id,
            String appId,
            String name,
            String category,
            String status
    ) {
    }

    public record CategoryOption(
            Long id,
            String name,
            Integer sortOrder
    ) {
    }

    public record SlotControl(
            String slotCode,
            String name,
            String pageCode,
            String positionCode,
            Boolean enabled
    ) {
    }

    public record ScopePreview(
            String scopeCode,
            String scopeName,
            Integer draftCount,
            Integer liveCount,
            Integer addedCount,
            Integer removedCount,
            Integer changedCount,
            LocalDateTime nextScheduledAt,
            Boolean pendingChanges
    ) {
    }

    public record PublishOrderSummary(
            Long id,
            String orderNo,
            String scopeCode,
            String scopeName,
            String status,
            LocalDateTime effectiveAt,
            String reason,
            String createdByName,
            LocalDateTime createdAt,
            Boolean liveNow
    ) {
    }

    public record ExperimentConfig(
            Long id,
            String slotCode,
            String scopeCode,
            String experimentName,
            Integer trafficPercent,
            String status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String note,
            Integer variantCount
    ) {
    }
}
