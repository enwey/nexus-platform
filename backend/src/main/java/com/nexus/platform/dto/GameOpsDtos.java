package com.nexus.platform.dto;

public class GameOpsDtos {
    public record GameOpsProfileResponse(
            Long gameId,
            String studioName,
            String playerCountText,
            String runtimeBannerUrl,
            String runtimeLogoUrl,
            String shareTitle,
            String shareSubtitle,
            String shareImageUrl,
            String discoverCardCoverUrl,
            String discoverCardLogoUrl
    ) {}

    public record GameOpsProfileUpdateRequest(
            String studioName,
            String playerCountText,
            String runtimeBannerUrl,
            String runtimeLogoUrl,
            String shareTitle,
            String shareSubtitle,
            String shareImageUrl,
            String discoverCardCoverUrl,
            String discoverCardLogoUrl
    ) {}

    public record RuntimeProfileResponse(
            String appId,
            String gameName,
            String studioName,
            String playerCountText,
            String runtimeBannerUrl,
            String runtimeLogoUrl,
            String shareTitle,
            String shareSubtitle,
            String shareImageUrl
    ) {}
}
