package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

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
            String discoverCardLogoUrl,
            String marketingTagline,
            String marketingSummary,
            List<String> featureHighlights,
            String targetAudience,
            String supportEmail,
            String supportUrl,
            String communityUrl,
            String complianceNote,
            String operationsStatus
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
            String discoverCardLogoUrl,
            String marketingTagline,
            String marketingSummary,
            List<String> featureHighlights,
            String targetAudience,
            String supportEmail,
            String supportUrl,
            String communityUrl,
            String complianceNote,
            String operationsStatus
    ) {}

    public record GameMediaAssetResponse(
            Long id,
            Long gameId,
            Long versionId,
            String assetGroup,
            String assetRole,
            String mediaType,
            String assetStatus,
            String title,
            String description,
            String url,
            String actionTitle,
            String actionUrl,
            Integer width,
            Integer height,
            Long sizeBytes,
            String locale,
            Integer sortOrder,
            Boolean primary,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record GameMediaAssetUpsertRequest(
            Long assetId,
            Long versionId,
            String assetGroup,
            String assetRole,
            String mediaType,
            String assetStatus,
            String title,
            String description,
            String url,
            String actionTitle,
            String actionUrl,
            Integer width,
            Integer height,
            Long sizeBytes,
            String locale,
            Integer sortOrder,
            Boolean primary
    ) {}

    public record GameMediaAssetDeleteRequest(
            String confirmText
    ) {}

    public record RuntimeProfileResponse(
            String appId,
            String gameName,
            String studioName,
            String playerCountText,
            Long packageSizeBytes,
            Long categoryPlayerCount,
            Integer categoryRank,
            String categoryName,
            String runtimeBannerUrl,
            String runtimeLogoUrl,
            String shareTitle,
            String shareSubtitle,
            String shareImageUrl
    ) {}
}
