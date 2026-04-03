package com.nexus.platform.dto;

import java.util.List;

public record OpsDiscoverConfigResponse(
        HeroConfig hero,
        List<String> rankedAppIds,
        List<String> newbieAppIds,
        List<String> everyoneAppIds,
        List<SimpleGameItem> availableGames
) {
    public record HeroConfig(
            String appId,
            String title,
            String subtitle,
            String badgeText,
            String coverUrl
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
}
