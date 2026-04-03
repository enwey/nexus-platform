package com.nexus.platform.dto;

import java.util.List;

public record OpsDiscoverConfigUpdateRequest(
        HeroConfig hero,
        List<String> rankedAppIds,
        List<String> newbieAppIds,
        List<String> everyoneAppIds
) {
    public record HeroConfig(
            String appId,
            String title,
            String subtitle,
            String badgeText,
            String coverUrl
    ) {
    }
}
