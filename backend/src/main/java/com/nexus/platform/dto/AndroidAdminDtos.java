package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AndroidAdminDtos {
    public record AndroidOverview(
            long totalGames,
            long approvedGames,
            long pendingGames,
            long processingGames,
            long rejectedGames,
            int bridgeImplementedCount,
            int bridgeStubCount
    ) {}

    public record AndroidRuntimeConfig(
            String apiBaseUrl,
            String assetHost,
            String defaultLanguage,
            boolean debugUseMockData,
            boolean enableSyncBridge,
            boolean allowCleartextTraffic,
            String webViewMixedContentMode,
            String webViewCacheMode,
            int maxZipSizeMb,
            String startupRoutePolicy,
            String updatedBy,
            LocalDateTime updatedAt
    ) {}

    public record AndroidBridgeApiItem(
            String apiName,
            String supportStatus,
            boolean syncSupported,
            String module,
            String notes
    ) {}

    public record AndroidGameAssetRow(
            Long gameId,
            String appId,
            String gameName,
            String version,
            String status,
            String downloadUrl,
            String md5,
            boolean runtimeReady,
            LocalDateTime updatedAt
    ) {}

    public record AndroidConsolePayload(
            AndroidOverview overview,
            AndroidRuntimeConfig config,
            List<AndroidBridgeApiItem> bridgeApis,
            List<AndroidGameAssetRow> gameAssets
    ) {}

    public record AndroidRuntimeConfigUpdateRequest(
            String apiBaseUrl,
            String assetHost,
            String defaultLanguage,
            Boolean debugUseMockData,
            Boolean enableSyncBridge,
            Boolean allowCleartextTraffic,
            String webViewMixedContentMode,
            String webViewCacheMode,
            Integer maxZipSizeMb,
            String startupRoutePolicy
    ) {}
}
