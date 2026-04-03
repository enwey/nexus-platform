package com.nexus.platform.dto;

import com.nexus.platform.entity.Game;
import java.util.List;

public record DiscoverFeedItem(
        String appId,
        String name,
        String description,
        String iconUrl,
        String coverUrl,
        String logoUrl,
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
                game.getIconUrl(),
                game.getIconUrl(),
                game.getDownloadUrl(),
                game.getVersion(),
                game.getMd5(),
                hotScore,
                category,
                tags
        );
    }

    public DiscoverFeedItem withVisualOverrides(String overrideCoverUrl, String overrideLogoUrl) {
        return new DiscoverFeedItem(
                appId,
                name,
                description,
                iconUrl,
                blankToNull(overrideCoverUrl) == null ? coverUrl : overrideCoverUrl,
                blankToNull(overrideLogoUrl) == null ? logoUrl : overrideLogoUrl,
                downloadUrl,
                version,
                md5,
                hotScore,
                category,
                tags
        );
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
