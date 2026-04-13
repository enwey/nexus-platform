package com.nexus.platform.dto;

import java.util.List;

public record DiscoverHomeResponse(
        DiscoverHeroCard hero,
        DiscoverHeroCard libraryTopBanner,
        List<String> categories,
        List<DiscoverFeedItem> rankedGames,
        List<DiscoverFeedItem> newbieMustPlay,
        List<DiscoverFeedItem> everyonePlaying
) {
}
