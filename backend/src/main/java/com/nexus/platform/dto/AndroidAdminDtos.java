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
            long runtimeReadyGames,
            int bridgeImplementedCount,
            int bridgePartialCount
    ) {}

    public record AndroidRuntimeConfig(
            String apiBaseUrl,
            String assetHost,
            String defaultLanguage,
            String minimumSupportedVersion,
            boolean forceUpdateEnabled,
            String grayReleaseDescription,
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

    public record AndroidChannelRuleDto(
            Long id,
            String channelCode,
            String channelName,
            String status,
            String minimumVersion,
            boolean forceUpdateEnabled,
            boolean grayReleaseEnabled,
            int trafficPercentage,
            String note,
            String updatedBy,
            LocalDateTime updatedAt
    ) {}

    public record AndroidFeatureToggleDto(
            Long id,
            String featureKey,
            String featureName,
            String status,
            String scope,
            String owner,
            String note,
            String updatedBy,
            LocalDateTime updatedAt
    ) {}

    public record AndroidAbExperimentDto(
            Long id,
            String experimentKey,
            String experimentName,
            String status,
            String layerKey,
            String owner,
            String channelCode,
            String minAppVersion,
            String maxAppVersion,
            int trafficPercentage,
            String variantAName,
            int variantAPercentage,
            String variantBName,
            int variantBPercentage,
            String hypothesis,
            String successMetric,
            String note,
            String updatedBy,
            LocalDateTime updatedAt
    ) {}

    public record AndroidGrayReleasePlanDto(
            Long id,
            String planCode,
            String planName,
            String status,
            String channelCode,
            String minAppVersion,
            String maxAppVersion,
            int overallTrafficPercentage,
            int newUserPercentage,
            int returningUserPercentage,
            int whitelistPercentage,
            String regionCode,
            String deviceTier,
            String fallbackPolicy,
            String note,
            String updatedBy,
            LocalDateTime updatedAt
    ) {}

    public record AndroidCompatibilityBlacklistDto(
            Long id,
            String ruleCode,
            String status,
            String targetType,
            String targetValue,
            String minAppVersion,
            String maxAppVersion,
            Integer minSdkInt,
            Integer maxSdkInt,
            String reason,
            String note,
            String updatedBy,
            LocalDateTime updatedAt
    ) {}

    public record AndroidPageCircuitBreakerDto(
            Long id,
            String pageKey,
            String pageName,
            String status,
            String audienceScope,
            String channelCode,
            String minAppVersion,
            String maxAppVersion,
            String degradeMode,
            String reason,
            String note,
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

    public record AndroidHostCapability(
            String capabilityKey,
            String displayName,
            String category,
            String status,
            String sourceModule,
            String summary
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
            List<AndroidChannelRuleDto> channelRules,
            List<AndroidFeatureToggleDto> featureToggles,
            List<AndroidAbExperimentDto> abExperiments,
            List<AndroidGrayReleasePlanDto> grayReleasePlans,
            List<AndroidCompatibilityBlacklistDto> compatibilityBlacklists,
            List<AndroidPageCircuitBreakerDto> pageCircuitBreakers,
            List<AndroidHostCapability> hostCapabilities,
            List<AndroidBridgeApiItem> bridgeApis,
            List<AndroidGameAssetRow> gameAssets
    ) {}

    public record AndroidRuntimeConfigUpdateRequest(
            String apiBaseUrl,
            String assetHost,
            String defaultLanguage,
            String minimumSupportedVersion,
            Boolean forceUpdateEnabled,
            String grayReleaseDescription,
            Boolean debugUseMockData,
            Boolean enableSyncBridge,
            Boolean allowCleartextTraffic,
            String webViewMixedContentMode,
            String webViewCacheMode,
            Integer maxZipSizeMb,
            String startupRoutePolicy
    ) {}

    public record AndroidChannelRuleUpsertRequest(
            String channelCode,
            String channelName,
            String status,
            String minimumVersion,
            Boolean forceUpdateEnabled,
            Boolean grayReleaseEnabled,
            Integer trafficPercentage,
            String note
    ) {}

    public record AndroidFeatureToggleUpsertRequest(
            String featureKey,
            String featureName,
            String status,
            String scope,
            String owner,
            String note
    ) {}

    public record AndroidAbExperimentUpsertRequest(
            String experimentKey,
            String experimentName,
            String status,
            String layerKey,
            String owner,
            String channelCode,
            String minAppVersion,
            String maxAppVersion,
            Integer trafficPercentage,
            String variantAName,
            Integer variantAPercentage,
            String variantBName,
            Integer variantBPercentage,
            String hypothesis,
            String successMetric,
            String note
    ) {}

    public record AndroidGrayReleasePlanUpsertRequest(
            String planCode,
            String planName,
            String status,
            String channelCode,
            String minAppVersion,
            String maxAppVersion,
            Integer overallTrafficPercentage,
            Integer newUserPercentage,
            Integer returningUserPercentage,
            Integer whitelistPercentage,
            String regionCode,
            String deviceTier,
            String fallbackPolicy,
            String note
    ) {}

    public record AndroidCompatibilityBlacklistUpsertRequest(
            String ruleCode,
            String status,
            String targetType,
            String targetValue,
            String minAppVersion,
            String maxAppVersion,
            Integer minSdkInt,
            Integer maxSdkInt,
            String reason,
            String note
    ) {}

    public record AndroidPageCircuitBreakerUpsertRequest(
            String pageKey,
            String pageName,
            String status,
            String audienceScope,
            String channelCode,
            String minAppVersion,
            String maxAppVersion,
            String degradeMode,
            String reason,
            String note
    ) {}
}
