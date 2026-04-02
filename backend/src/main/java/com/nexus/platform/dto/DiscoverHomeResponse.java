package com.nexus.platform.dto;

import java.util.List;

public record DiscoverHomeResponse(
        DiscoverHeroCard hero,
        List<DiscoverFeedItem> rankedGames,
        List<DiscoverFeedItem> newbieMustPlay,
        List<DiscoverFeedItem> everyonePlaying
) {
}
