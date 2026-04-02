package com.nexus.platform.dto;

import com.nexus.platform.entity.Game;
import java.util.List;

public record DiscoverFeedItem(
        String appId,
        String name,
        String description,
        String iconUrl,
        String downloadUrl,
        String version,
        String md5,
        long hotScore,
        String category,
        List<String> tags
) {
    public static DiscoverFeedItem from(Game game, long hotScore, String category, List<String> tags) {
        return new DiscoverFeedItem(
                game.getAppId(),
                game.getName(),
                game.getDescription(),
                game.getIconUrl(),
                game.getDownloadUrl(),
                game.getVersion(),
                game.getMd5(),
                hotScore,
                category,
                tags
        );
    }
}
