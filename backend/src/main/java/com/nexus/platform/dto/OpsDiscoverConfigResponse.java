package com.nexus.platform.dto;

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
        List<CategoryOption> categoryOptions
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
            String appId,
            String title,
            String subtitle,
            String badgeText,
            String coverUrl
    ) {
    }

    public record CommunityItemConfig(
            String appId,
            String cardCategory,
            String cardTitle,
            String coverUrl,
            String articleTag,
            String articleTitle,
            String articleBody,
            String actionText
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
}
