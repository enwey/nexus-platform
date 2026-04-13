package com.nexus.platform.dto;

public record DiscoverCommunityItem(
        String appId,
        String gameName,
        String gameCategory,
        String gameIconUrl,
        String cardCategory,
        String cardTitle,
        String coverUrl,
        String articleTag,
        String articleTitle,
        String articleBody,
        String actionText
) {
}
