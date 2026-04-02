package com.nexus.platform.dto;

public record DiscoverHeroCard(
        String appId,
        String title,
        String subtitle,
        String coverUrl,
        String badgeText,
        String jumpType
) {
}
