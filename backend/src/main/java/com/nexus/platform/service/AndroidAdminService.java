package com.nexus.platform.service;

import com.nexus.platform.dto.AndroidAdminDtos.AndroidBridgeApiItem;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidChannelRuleDto;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidChannelRuleUpsertRequest;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidAbExperimentDto;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidAbExperimentUpsertRequest;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidCompatibilityBlacklistDto;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidCompatibilityBlacklistUpsertRequest;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidConsolePayload;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidPageCircuitBreakerDto;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidPageCircuitBreakerUpsertRequest;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidFeatureToggleDto;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidFeatureToggleUpsertRequest;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidGameAssetRow;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidGrayReleasePlanDto;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidGrayReleasePlanUpsertRequest;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidHostCapability;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidOverview;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidRuntimeConfig;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidRuntimeConfigUpdateRequest;
import com.nexus.platform.entity.AndroidAbExperiment;
import com.nexus.platform.entity.AndroidCompatibilityBlacklist;
import com.nexus.platform.entity.AndroidChannelRule;
import com.nexus.platform.entity.AndroidFeatureToggle;
import com.nexus.platform.entity.AndroidGrayReleasePlan;
import com.nexus.platform.entity.AndroidPageCircuitBreaker;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.AndroidAbExperimentRepository;
import com.nexus.platform.repository.AndroidCompatibilityBlacklistRepository;
import com.nexus.platform.repository.AndroidChannelRuleRepository;
import com.nexus.platform.repository.AndroidFeatureToggleRepository;
import com.nexus.platform.repository.AndroidGrayReleasePlanRepository;
import com.nexus.platform.repository.AndroidPageCircuitBreakerRepository;
import com.nexus.platform.repository.GameRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AndroidAdminService {
    private static final Set<String> CHANNEL_STATUSES = Set.of("ACTIVE", "PAUSED", "BLOCKED");
    private static final Set<String> FEATURE_STATUSES = Set.of("ENABLED", "DISABLED", "GRAY");
    private static final Set<String> FEATURE_SCOPES = Set.of("GLOBAL", "ANDROID_ONLY", "RUNTIME_ONLY", "BRIDGE_ONLY");
    private static final Set<String> EXPERIMENT_STATUSES = Set.of("DRAFT", "RUNNING", "PAUSED", "ARCHIVED");
    private static final Set<String> GRAY_PLAN_STATUSES = Set.of("DRAFT", "ACTIVE", "PAUSED", "STOPPED");
    private static final Set<String> GRAY_FALLBACK_POLICIES = Set.of("ROLLBACK", "HOLD", "FORCE_UPDATE");
    private static final Set<String> BLACKLIST_STATUSES = Set.of("ACTIVE", "PAUSED");
    private static final Set<String> BLACKLIST_TARGET_TYPES = Set.of("DEVICE_MODEL", "SDK_INT", "CHANNEL_CODE", "APP_VERSION", "MANUFACTURER");
    private static final Set<String> BREAKER_STATUSES = Set.of("ENABLED", "DISABLED", "GRAY");
    private static final Set<String> BREAKER_AUDIENCES = Set.of("ALL", "GRAY_ONLY", "CHANNEL_ONLY");
    private static final Set<String> BREAKER_DEGRADE_MODES = Set.of("HIDE", "NATIVE_FALLBACK", "MAINTENANCE");

    private final GameRepository gameRepository;
    private final AndroidChannelRuleRepository androidChannelRuleRepository;
    private final AndroidFeatureToggleRepository androidFeatureToggleRepository;
    private final AndroidAbExperimentRepository androidAbExperimentRepository;
    private final AndroidGrayReleasePlanRepository androidGrayReleasePlanRepository;
    private final AndroidCompatibilityBlacklistRepository androidCompatibilityBlacklistRepository;
    private final AndroidPageCircuitBreakerRepository androidPageCircuitBreakerRepository;
    private final AuditLogService auditLogService;
    private final AtomicReference<AndroidRuntimeConfig> runtimeConfigRef;

    public AndroidAdminService(
            GameRepository gameRepository,
            AndroidChannelRuleRepository androidChannelRuleRepository,
            AndroidFeatureToggleRepository androidFeatureToggleRepository,
            AndroidAbExperimentRepository androidAbExperimentRepository,
            AndroidGrayReleasePlanRepository androidGrayReleasePlanRepository,
            AndroidCompatibilityBlacklistRepository androidCompatibilityBlacklistRepository,
            AndroidPageCircuitBreakerRepository androidPageCircuitBreakerRepository,
            AuditLogService auditLogService,
            @Value("${platform.public-base-url}") String platformApiBaseUrl
    ) {
        this.gameRepository = gameRepository;
        this.androidChannelRuleRepository = androidChannelRuleRepository;
        this.androidFeatureToggleRepository = androidFeatureToggleRepository;
        this.androidAbExperimentRepository = androidAbExperimentRepository;
        this.androidGrayReleasePlanRepository = androidGrayReleasePlanRepository;
        this.androidCompatibilityBlacklistRepository = androidCompatibilityBlacklistRepository;
        this.androidPageCircuitBreakerRepository = androidPageCircuitBreakerRepository;
        this.auditLogService = auditLogService;
        this.runtimeConfigRef = new AtomicReference<>(new AndroidRuntimeConfig(
                platformApiBaseUrl,
                "https://appassets.androidplatform.net/assets/",
                "zh-TW",
                "1.0.0",
                false,
                "disabled",
                true,
                true,
                false,
                "MIXED_CONTENT_NEVER_ALLOW",
                "LOAD_DEFAULT",
                200,
                "SPLASH_DIRECT_TO_MAIN",
                "system",
                LocalDateTime.now()
        ));
        seedRuntimeControls();
    }

    public AndroidConsolePayload getConsolePayload() {
        return new AndroidConsolePayload(
            buildOverview(),
            runtimeConfigRef.get(),
            getChannelRules(),
            getFeatureToggles(),
            getAbExperiments(),
            getGrayReleasePlans(),
            getCompatibilityBlacklists(),
            getPageCircuitBreakers(),
            hostCapabilities(),
            bridgeApis(),
            gameAssets()
        );
    }

    public AndroidRuntimeConfig getRuntimeConfig() {
        return runtimeConfigRef.get();
    }

    public AndroidRuntimeConfig updateRuntimeConfig(AndroidRuntimeConfigUpdateRequest request, User currentUser, String requestUri) {
        AndroidRuntimeConfig current = runtimeConfigRef.get();
        String minimumSupportedVersion = valueOrDefault(request.minimumSupportedVersion(), current.minimumSupportedVersion());
        String grayReleaseDescription = valueOrDefault(request.grayReleaseDescription(), current.grayReleaseDescription());
        AndroidRuntimeConfig next = new AndroidRuntimeConfig(
                valueOrDefault(request.apiBaseUrl(), current.apiBaseUrl()),
                valueOrDefault(request.assetHost(), current.assetHost()),
                valueOrDefault(request.defaultLanguage(), current.defaultLanguage()),
                minimumSupportedVersion,
                boolOrDefault(request.forceUpdateEnabled(), current.forceUpdateEnabled()),
                grayReleaseDescription,
                boolOrDefault(request.debugUseMockData(), current.debugUseMockData()),
                boolOrDefault(request.enableSyncBridge(), current.enableSyncBridge()),
                boolOrDefault(request.allowCleartextTraffic(), current.allowCleartextTraffic()),
                valueOrDefault(request.webViewMixedContentMode(), current.webViewMixedContentMode()),
                valueOrDefault(request.webViewCacheMode(), current.webViewCacheMode()),
                intOrDefault(request.maxZipSizeMb(), current.maxZipSizeMb()),
                valueOrDefault(request.startupRoutePolicy(), current.startupRoutePolicy()),
                currentUser == null ? "unknown" : currentUser.getUsername(),
                LocalDateTime.now()
        );
        runtimeConfigRef.set(next);
        auditLogService.logOpsAudit("ANDROID_RUNTIME_CONFIG_UPDATE", currentUser, null, "ANDROID_RUNTIME_CONFIG", true, minimumSupportedVersion, requestUri);
        return next;
    }

    public List<AndroidBridgeApiItem> getBridgeApis() {
        return bridgeApis();
    }

    public List<AndroidGameAssetRow> getGameAssets() {
        return gameAssets();
    }

    public List<AndroidChannelRuleDto> getChannelRules() {
        seedRuntimeControls();
        return androidChannelRuleRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toChannelRuleDto).toList();
    }

    public AndroidChannelRuleDto upsertChannelRule(AndroidChannelRuleUpsertRequest request, User currentUser, String requestUri) {
        String channelCode = normalizeCode(request == null ? null : request.channelCode(), 32);
        if (channelCode == null) {
            throw new IllegalArgumentException("Invalid channel code");
        }
        String channelName = trim(request == null ? null : request.channelName(), 64);
        if (channelName == null || channelName.length() < 2) {
            throw new IllegalArgumentException("Invalid channel name");
        }
        String status = normalizeEnum(request == null ? null : request.status(), CHANNEL_STATUSES, "ACTIVE");
        int trafficPercentage = request == null || request.trafficPercentage() == null ? 100 : request.trafficPercentage();
        if (trafficPercentage < 0 || trafficPercentage > 100) {
            throw new IllegalArgumentException("Traffic percentage must be between 0 and 100");
        }
        AndroidChannelRule entity = androidChannelRuleRepository.findByChannelCode(channelCode).orElseGet(AndroidChannelRule::new);
        entity.setChannelCode(channelCode);
        entity.setChannelName(channelName);
        entity.setStatus(status);
        entity.setMinimumVersion(trim(request == null ? null : request.minimumVersion(), 32));
        entity.setForceUpdateEnabled(request != null && Boolean.TRUE.equals(request.forceUpdateEnabled()));
        entity.setGrayReleaseEnabled(request != null && Boolean.TRUE.equals(request.grayReleaseEnabled()));
        entity.setTrafficPercentage(trafficPercentage);
        entity.setNote(trim(request == null ? null : request.note(), 256));
        entity.setUpdatedBy(currentUser == null ? "unknown" : currentUser.getUsername());
        AndroidChannelRule saved = androidChannelRuleRepository.save(entity);
        auditLogService.logOpsAudit("ANDROID_CHANNEL_RULE_UPSERT", currentUser, null, channelCode, true, status, requestUri);
        return toChannelRuleDto(saved);
    }

    public List<AndroidFeatureToggleDto> getFeatureToggles() {
        seedRuntimeControls();
        return androidFeatureToggleRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toFeatureToggleDto).toList();
    }

    public AndroidFeatureToggleDto upsertFeatureToggle(AndroidFeatureToggleUpsertRequest request, User currentUser, String requestUri) {
        String featureKey = normalizeCode(request == null ? null : request.featureKey(), 64);
        if (featureKey == null) {
            throw new IllegalArgumentException("Invalid feature key");
        }
        String featureName = trim(request == null ? null : request.featureName(), 128);
        if (featureName == null || featureName.length() < 2) {
            throw new IllegalArgumentException("Invalid feature name");
        }
        String status = normalizeEnum(request == null ? null : request.status(), FEATURE_STATUSES, "ENABLED");
        String scope = normalizeEnum(request == null ? null : request.scope(), FEATURE_SCOPES, "GLOBAL");
        AndroidFeatureToggle entity = androidFeatureToggleRepository.findByFeatureKey(featureKey).orElseGet(AndroidFeatureToggle::new);
        entity.setFeatureKey(featureKey);
        entity.setFeatureName(featureName);
        entity.setStatus(status);
        entity.setScope(scope);
        entity.setOwner(trim(request == null ? null : request.owner(), 128));
        entity.setNote(trim(request == null ? null : request.note(), 256));
        entity.setUpdatedBy(currentUser == null ? "unknown" : currentUser.getUsername());
        AndroidFeatureToggle saved = androidFeatureToggleRepository.save(entity);
        auditLogService.logOpsAudit("ANDROID_FEATURE_TOGGLE_UPSERT", currentUser, null, featureKey, true, status, requestUri);
        return toFeatureToggleDto(saved);
    }

    public List<AndroidAbExperimentDto> getAbExperiments() {
        seedRuntimeControls();
        return androidAbExperimentRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toAbExperimentDto).toList();
    }

    public AndroidAbExperimentDto upsertAbExperiment(AndroidAbExperimentUpsertRequest request, User currentUser, String requestUri) {
        String experimentKey = normalizeCode(request == null ? null : request.experimentKey(), 64);
        if (experimentKey == null) {
            throw new IllegalArgumentException("Invalid experiment key");
        }
        String experimentName = trim(request == null ? null : request.experimentName(), 128);
        if (experimentName == null || experimentName.length() < 2) {
            throw new IllegalArgumentException("Invalid experiment name");
        }
        String layerKey = normalizeCode(request == null ? null : request.layerKey(), 64);
        if (layerKey == null) {
            throw new IllegalArgumentException("Invalid layer key");
        }
        int trafficPercentage = request == null || request.trafficPercentage() == null ? 10 : request.trafficPercentage();
        int variantAPercentage = request == null || request.variantAPercentage() == null ? 50 : request.variantAPercentage();
        int variantBPercentage = request == null || request.variantBPercentage() == null ? 50 : request.variantBPercentage();
        if (trafficPercentage < 1 || trafficPercentage > 100) {
            throw new IllegalArgumentException("Experiment traffic percentage must be between 1 and 100");
        }
        if (variantAPercentage < 0 || variantAPercentage > 100 || variantBPercentage < 0 || variantBPercentage > 100) {
            throw new IllegalArgumentException("Variant percentage must be between 0 and 100");
        }
        if (variantAPercentage + variantBPercentage != 100) {
            throw new IllegalArgumentException("Variant percentage sum must be 100");
        }
        String variantAName = trim(request == null ? null : request.variantAName(), 64);
        String variantBName = trim(request == null ? null : request.variantBName(), 64);
        if (variantAName == null || variantBName == null) {
            throw new IllegalArgumentException("Both variant names are required");
        }
        AndroidAbExperiment entity = androidAbExperimentRepository.findByExperimentKey(experimentKey).orElseGet(AndroidAbExperiment::new);
        entity.setExperimentKey(experimentKey);
        entity.setExperimentName(experimentName);
        entity.setStatus(normalizeEnum(request == null ? null : request.status(), EXPERIMENT_STATUSES, "DRAFT"));
        entity.setLayerKey(layerKey);
        entity.setOwner(trim(request == null ? null : request.owner(), 128));
        entity.setChannelCode(trim(request == null ? null : request.channelCode(), 32));
        entity.setMinAppVersion(trim(request == null ? null : request.minAppVersion(), 32));
        entity.setMaxAppVersion(trim(request == null ? null : request.maxAppVersion(), 32));
        entity.setTrafficPercentage(trafficPercentage);
        entity.setVariantAName(variantAName);
        entity.setVariantAPercentage(variantAPercentage);
        entity.setVariantBName(variantBName);
        entity.setVariantBPercentage(variantBPercentage);
        entity.setHypothesis(trim(request == null ? null : request.hypothesis(), 256));
        entity.setSuccessMetric(trim(request == null ? null : request.successMetric(), 128));
        entity.setNote(trim(request == null ? null : request.note(), 256));
        entity.setUpdatedBy(currentUser == null ? "unknown" : currentUser.getUsername());
        AndroidAbExperiment saved = androidAbExperimentRepository.save(entity);
        auditLogService.logOpsAudit("ANDROID_AB_EXPERIMENT_UPSERT", currentUser, null, experimentKey, true, entity.getStatus(), requestUri);
        return toAbExperimentDto(saved);
    }

    public List<AndroidGrayReleasePlanDto> getGrayReleasePlans() {
        seedRuntimeControls();
        return androidGrayReleasePlanRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toGrayReleasePlanDto).toList();
    }

    public AndroidGrayReleasePlanDto upsertGrayReleasePlan(AndroidGrayReleasePlanUpsertRequest request, User currentUser, String requestUri) {
        String planCode = normalizeCode(request == null ? null : request.planCode(), 64);
        if (planCode == null) {
            throw new IllegalArgumentException("Invalid plan code");
        }
        String planName = trim(request == null ? null : request.planName(), 128);
        if (planName == null || planName.length() < 2) {
            throw new IllegalArgumentException("Invalid plan name");
        }
        String channelCode = normalizeCode(request == null ? null : request.channelCode(), 32);
        if (channelCode == null) {
            throw new IllegalArgumentException("Invalid channel code");
        }
        int overallTrafficPercentage = request == null || request.overallTrafficPercentage() == null ? 10 : request.overallTrafficPercentage();
        int newUserPercentage = request == null || request.newUserPercentage() == null ? 50 : request.newUserPercentage();
        int returningUserPercentage = request == null || request.returningUserPercentage() == null ? 50 : request.returningUserPercentage();
        int whitelistPercentage = request == null || request.whitelistPercentage() == null ? 0 : request.whitelistPercentage();
        if (overallTrafficPercentage < 1 || overallTrafficPercentage > 100) {
            throw new IllegalArgumentException("Overall traffic percentage must be between 1 and 100");
        }
        if (newUserPercentage < 0 || newUserPercentage > 100
                || returningUserPercentage < 0 || returningUserPercentage > 100
                || whitelistPercentage < 0 || whitelistPercentage > 100) {
            throw new IllegalArgumentException("All gray percentages must be between 0 and 100");
        }
        if (newUserPercentage + returningUserPercentage != 100) {
            throw new IllegalArgumentException("New and returning user percentage sum must be 100");
        }
        if (whitelistPercentage > overallTrafficPercentage) {
            throw new IllegalArgumentException("Whitelist percentage cannot exceed overall traffic");
        }
        AndroidGrayReleasePlan entity = androidGrayReleasePlanRepository.findByPlanCode(planCode).orElseGet(AndroidGrayReleasePlan::new);
        entity.setPlanCode(planCode);
        entity.setPlanName(planName);
        entity.setStatus(normalizeEnum(request == null ? null : request.status(), GRAY_PLAN_STATUSES, "DRAFT"));
        entity.setChannelCode(channelCode);
        entity.setMinAppVersion(trim(request == null ? null : request.minAppVersion(), 32));
        entity.setMaxAppVersion(trim(request == null ? null : request.maxAppVersion(), 32));
        entity.setOverallTrafficPercentage(overallTrafficPercentage);
        entity.setNewUserPercentage(newUserPercentage);
        entity.setReturningUserPercentage(returningUserPercentage);
        entity.setWhitelistPercentage(whitelistPercentage);
        entity.setRegionCode(trim(request == null ? null : request.regionCode(), 32));
        entity.setDeviceTier(trim(request == null ? null : request.deviceTier(), 32));
        entity.setFallbackPolicy(normalizeEnum(request == null ? null : request.fallbackPolicy(), GRAY_FALLBACK_POLICIES, "ROLLBACK"));
        entity.setNote(trim(request == null ? null : request.note(), 256));
        entity.setUpdatedBy(currentUser == null ? "unknown" : currentUser.getUsername());
        AndroidGrayReleasePlan saved = androidGrayReleasePlanRepository.save(entity);
        auditLogService.logOpsAudit("ANDROID_GRAY_RELEASE_PLAN_UPSERT", currentUser, null, planCode, true, entity.getStatus(), requestUri);
        return toGrayReleasePlanDto(saved);
    }

    public List<AndroidCompatibilityBlacklistDto> getCompatibilityBlacklists() {
        seedRuntimeControls();
        return androidCompatibilityBlacklistRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toCompatibilityBlacklistDto).toList();
    }

    public AndroidCompatibilityBlacklistDto upsertCompatibilityBlacklist(
            AndroidCompatibilityBlacklistUpsertRequest request,
            User currentUser,
            String requestUri
    ) {
        String ruleCode = normalizeCode(request == null ? null : request.ruleCode(), 64);
        if (ruleCode == null) {
            throw new IllegalArgumentException("Invalid rule code");
        }
        String targetType = normalizeEnum(request == null ? null : request.targetType(), BLACKLIST_TARGET_TYPES, null);
        if (targetType == null) {
            throw new IllegalArgumentException("Invalid target type");
        }
        String targetValue = trim(request == null ? null : request.targetValue(), 128);
        if (targetValue == null || targetValue.length() < 2) {
            throw new IllegalArgumentException("Invalid target value");
        }
        String reason = trim(request == null ? null : request.reason(), 256);
        if (reason == null || reason.length() < 2) {
            throw new IllegalArgumentException("Invalid reason");
        }
        Integer minSdkInt = request == null ? null : request.minSdkInt();
        Integer maxSdkInt = request == null ? null : request.maxSdkInt();
        if (minSdkInt != null && minSdkInt < 1) {
            throw new IllegalArgumentException("Invalid min sdk");
        }
        if (maxSdkInt != null && maxSdkInt < 1) {
            throw new IllegalArgumentException("Invalid max sdk");
        }
        if (minSdkInt != null && maxSdkInt != null && minSdkInt > maxSdkInt) {
            throw new IllegalArgumentException("Min sdk cannot be greater than max sdk");
        }
        AndroidCompatibilityBlacklist entity = androidCompatibilityBlacklistRepository.findByRuleCode(ruleCode).orElseGet(AndroidCompatibilityBlacklist::new);
        entity.setRuleCode(ruleCode);
        entity.setStatus(normalizeEnum(request == null ? null : request.status(), BLACKLIST_STATUSES, "ACTIVE"));
        entity.setTargetType(targetType);
        entity.setTargetValue(targetValue);
        entity.setMinAppVersion(trim(request == null ? null : request.minAppVersion(), 32));
        entity.setMaxAppVersion(trim(request == null ? null : request.maxAppVersion(), 32));
        entity.setMinSdkInt(minSdkInt);
        entity.setMaxSdkInt(maxSdkInt);
        entity.setReason(reason);
        entity.setNote(trim(request == null ? null : request.note(), 256));
        entity.setUpdatedBy(currentUser == null ? "unknown" : currentUser.getUsername());
        AndroidCompatibilityBlacklist saved = androidCompatibilityBlacklistRepository.save(entity);
        auditLogService.logOpsAudit("ANDROID_COMPAT_BLACKLIST_UPSERT", currentUser, null, ruleCode, true, entity.getStatus(), requestUri);
        return toCompatibilityBlacklistDto(saved);
    }

    public List<AndroidPageCircuitBreakerDto> getPageCircuitBreakers() {
        seedRuntimeControls();
        return androidPageCircuitBreakerRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toPageCircuitBreakerDto).toList();
    }

    public AndroidPageCircuitBreakerDto upsertPageCircuitBreaker(
            AndroidPageCircuitBreakerUpsertRequest request,
            User currentUser,
            String requestUri
    ) {
        String pageKey = normalizeCode(request == null ? null : request.pageKey(), 64);
        if (pageKey == null) {
            throw new IllegalArgumentException("Invalid page key");
        }
        String pageName = trim(request == null ? null : request.pageName(), 128);
        if (pageName == null || pageName.length() < 2) {
            throw new IllegalArgumentException("Invalid page name");
        }
        String reason = trim(request == null ? null : request.reason(), 256);
        if (reason == null || reason.length() < 2) {
            throw new IllegalArgumentException("Invalid reason");
        }
        AndroidPageCircuitBreaker entity = androidPageCircuitBreakerRepository.findByPageKey(pageKey).orElseGet(AndroidPageCircuitBreaker::new);
        entity.setPageKey(pageKey);
        entity.setPageName(pageName);
        entity.setStatus(normalizeEnum(request == null ? null : request.status(), BREAKER_STATUSES, "ENABLED"));
        entity.setAudienceScope(normalizeEnum(request == null ? null : request.audienceScope(), BREAKER_AUDIENCES, "ALL"));
        entity.setChannelCode(trim(request == null ? null : request.channelCode(), 32));
        entity.setMinAppVersion(trim(request == null ? null : request.minAppVersion(), 32));
        entity.setMaxAppVersion(trim(request == null ? null : request.maxAppVersion(), 32));
        entity.setDegradeMode(normalizeEnum(request == null ? null : request.degradeMode(), BREAKER_DEGRADE_MODES, "HIDE"));
        entity.setReason(reason);
        entity.setNote(trim(request == null ? null : request.note(), 256));
        entity.setUpdatedBy(currentUser == null ? "unknown" : currentUser.getUsername());
        AndroidPageCircuitBreaker saved = androidPageCircuitBreakerRepository.save(entity);
        auditLogService.logOpsAudit("ANDROID_PAGE_BREAKER_UPSERT", currentUser, null, pageKey, true, entity.getStatus(), requestUri);
        return toPageCircuitBreakerDto(saved);
    }

    private AndroidOverview buildOverview() {
        List<Game> games = gameRepository.findAll();
        long total = games.size();
        long approved = games.stream().filter(g -> g.getStatus() == Game.GameStatus.APPROVED).count();
        long pending = games.stream().filter(g -> g.getStatus() == Game.GameStatus.PENDING).count();
        long processing = games.stream().filter(g -> g.getStatus() == Game.GameStatus.PROCESSING).count();
        long rejected = games.stream().filter(g -> g.getStatus() == Game.GameStatus.REJECTED).count();
        long runtimeReady = games.stream()
                .filter(g -> g.getStatus() == Game.GameStatus.APPROVED)
                .filter(g -> g.getDownloadUrl() != null && !g.getDownloadUrl().isBlank())
                .filter(g -> g.getMd5() != null && !g.getMd5().isBlank())
                .count();
        int implemented = (int) bridgeApis().stream().filter(api -> "implemented".equals(api.supportStatus())).count();
        int partial = (int) bridgeApis().stream().filter(api -> !"implemented".equals(api.supportStatus())).count();
        return new AndroidOverview(total, approved, pending, processing, rejected, runtimeReady, implemented, partial);
    }

    private void seedRuntimeControls() {
        if (androidChannelRuleRepository.count() == 0) {
            saveSeedChannel("APPSTORE", "App Store", "ACTIVE", "1.0.0", false, false, 100, "Primary iOS style distribution channel");
            saveSeedChannel("ANDROID_CN", "Android CN", "ACTIVE", "1.0.0", false, true, 30, "Mainland Android gray rollout");
            saveSeedChannel("ANDROID_OVERSEA", "Android Oversea", "PAUSED", "1.1.0", true, false, 0, "Oversea rollout paused for compliance checks");
        }
        if (androidFeatureToggleRepository.count() == 0) {
            saveSeedFeature("runtime_capsule_menu", "Runtime Capsule Menu", "ENABLED", "RUNTIME_ONLY", "runtime", "Controls the in-game native overlay entry");
            saveSeedFeature("bridge_share_api", "Share Bridge API", "GRAY", "BRIDGE_ONLY", "bridge", "Share API kept in gray rollout");
            saveSeedFeature("webview_cleartext_fallback", "Cleartext Fallback", "DISABLED", "ANDROID_ONLY", "security", "Emergency fallback for legacy endpoints");
        }
        if (androidAbExperimentRepository.count() == 0) {
            saveSeedAbExperiment("DISCOVER_LAYOUT_V2", "Discover Layout V2", "RUNNING", "DISCOVER_FEED", "growth", "ANDROID_CN", "1.2.0", null, 20, "CONTROL", 50, "CARD_LAYOUT_V2", 50, "Validate card conversion uplift", "CTR uplift");
            saveSeedAbExperiment("PAYWALL_COPY_REFRESH", "Paywall Copy Refresh", "PAUSED", "PAYWALL_COPY", "monetization", null, "1.1.0", null, 10, "DEFAULT_COPY", 50, "VALUE_FIRST_COPY", 50, "Reduce paywall bounce", "Paywall conversion");
        }
        if (androidGrayReleasePlanRepository.count() == 0) {
            saveSeedGrayReleasePlan("ANDROID_CN_1_2_CANARY", "Android CN 1.2 Canary", "ACTIVE", "ANDROID_CN", "1.2.0", "1.2.9", 25, 60, 40, 5, "CN", "MID_HIGH", "ROLLBACK", "Primary staged rollout for mainland release");
            saveSeedGrayReleasePlan("APPSTORE_1_3_PREHEAT", "App Store 1.3 Preheat", "DRAFT", "APPSTORE", "1.3.0", null, 10, 50, 50, 0, "GLOBAL", "ALL", "HOLD", "Reserved for phased iOS style rollout");
        }
        if (androidCompatibilityBlacklistRepository.count() == 0) {
            saveSeedBlacklist("XIAOMI_ANDROID12_WEBVIEW", "ACTIVE", "DEVICE_MODEL", "2201123C", "1.0.0", "2.0.0", 31, 31, "Known crash loop on Android 12 WebView");
            saveSeedBlacklist("OVERSEA_LEGACY_CHANNEL", "PAUSED", "CHANNEL_CODE", "ANDROID_OVERSEA", null, null, null, null, "Legacy oversea channel pending compatibility retest");
        }
        if (androidPageCircuitBreakerRepository.count() == 0) {
            saveSeedPageBreaker("DISCOVER_FEED", "Discover Feed", "ENABLED", "ALL", null, null, null, "HIDE", "Default page state");
            saveSeedPageBreaker("COMMUNITY_TAB", "Community Tab", "GRAY", "GRAY_ONLY", "ANDROID_CN", "1.1.0", null, "NATIVE_FALLBACK", "Community tab in gray rollout");
        }
    }

    private void saveSeedChannel(String code, String name, String status, String minVersion, boolean forceUpdate, boolean grayRelease, int traffic, String note) {
        AndroidChannelRule rule = new AndroidChannelRule();
        rule.setChannelCode(code);
        rule.setChannelName(name);
        rule.setStatus(status);
        rule.setMinimumVersion(minVersion);
        rule.setForceUpdateEnabled(forceUpdate);
        rule.setGrayReleaseEnabled(grayRelease);
        rule.setTrafficPercentage(traffic);
        rule.setNote(note);
        rule.setUpdatedBy("system");
        androidChannelRuleRepository.save(rule);
    }

    private void saveSeedFeature(String key, String name, String status, String scope, String owner, String note) {
        AndroidFeatureToggle toggle = new AndroidFeatureToggle();
        toggle.setFeatureKey(key);
        toggle.setFeatureName(name);
        toggle.setStatus(status);
        toggle.setScope(scope);
        toggle.setOwner(owner);
        toggle.setNote(note);
        toggle.setUpdatedBy("system");
        androidFeatureToggleRepository.save(toggle);
    }

    private void saveSeedAbExperiment(
            String experimentKey,
            String experimentName,
            String status,
            String layerKey,
            String owner,
            String channelCode,
            String minVersion,
            String maxVersion,
            int trafficPercentage,
            String variantAName,
            int variantAPercentage,
            String variantBName,
            int variantBPercentage,
            String hypothesis,
            String successMetric
    ) {
        AndroidAbExperiment experiment = new AndroidAbExperiment();
        experiment.setExperimentKey(experimentKey);
        experiment.setExperimentName(experimentName);
        experiment.setStatus(status);
        experiment.setLayerKey(layerKey);
        experiment.setOwner(owner);
        experiment.setChannelCode(channelCode);
        experiment.setMinAppVersion(minVersion);
        experiment.setMaxAppVersion(maxVersion);
        experiment.setTrafficPercentage(trafficPercentage);
        experiment.setVariantAName(variantAName);
        experiment.setVariantAPercentage(variantAPercentage);
        experiment.setVariantBName(variantBName);
        experiment.setVariantBPercentage(variantBPercentage);
        experiment.setHypothesis(hypothesis);
        experiment.setSuccessMetric(successMetric);
        experiment.setUpdatedBy("system");
        androidAbExperimentRepository.save(experiment);
    }

    private void saveSeedGrayReleasePlan(
            String planCode,
            String planName,
            String status,
            String channelCode,
            String minVersion,
            String maxVersion,
            int overallTrafficPercentage,
            int newUserPercentage,
            int returningUserPercentage,
            int whitelistPercentage,
            String regionCode,
            String deviceTier,
            String fallbackPolicy,
            String note
    ) {
        AndroidGrayReleasePlan plan = new AndroidGrayReleasePlan();
        plan.setPlanCode(planCode);
        plan.setPlanName(planName);
        plan.setStatus(status);
        plan.setChannelCode(channelCode);
        plan.setMinAppVersion(minVersion);
        plan.setMaxAppVersion(maxVersion);
        plan.setOverallTrafficPercentage(overallTrafficPercentage);
        plan.setNewUserPercentage(newUserPercentage);
        plan.setReturningUserPercentage(returningUserPercentage);
        plan.setWhitelistPercentage(whitelistPercentage);
        plan.setRegionCode(regionCode);
        plan.setDeviceTier(deviceTier);
        plan.setFallbackPolicy(fallbackPolicy);
        plan.setNote(note);
        plan.setUpdatedBy("system");
        androidGrayReleasePlanRepository.save(plan);
    }

    private void saveSeedBlacklist(String ruleCode, String status, String targetType, String targetValue, String minVersion, String maxVersion, Integer minSdk, Integer maxSdk, String reason) {
        AndroidCompatibilityBlacklist item = new AndroidCompatibilityBlacklist();
        item.setRuleCode(ruleCode);
        item.setStatus(status);
        item.setTargetType(targetType);
        item.setTargetValue(targetValue);
        item.setMinAppVersion(minVersion);
        item.setMaxAppVersion(maxVersion);
        item.setMinSdkInt(minSdk);
        item.setMaxSdkInt(maxSdk);
        item.setReason(reason);
        item.setUpdatedBy("system");
        androidCompatibilityBlacklistRepository.save(item);
    }

    private void saveSeedPageBreaker(String pageKey, String pageName, String status, String audienceScope, String channelCode, String minVersion, String maxVersion, String degradeMode, String reason) {
        AndroidPageCircuitBreaker item = new AndroidPageCircuitBreaker();
        item.setPageKey(pageKey);
        item.setPageName(pageName);
        item.setStatus(status);
        item.setAudienceScope(audienceScope);
        item.setChannelCode(channelCode);
        item.setMinAppVersion(minVersion);
        item.setMaxAppVersion(maxVersion);
        item.setDegradeMode(degradeMode);
        item.setReason(reason);
        item.setUpdatedBy("system");
        androidPageCircuitBreakerRepository.save(item);
    }

    private AndroidChannelRuleDto toChannelRuleDto(AndroidChannelRule item) {
        return new AndroidChannelRuleDto(
                item.getId(),
                item.getChannelCode(),
                item.getChannelName(),
                item.getStatus(),
                item.getMinimumVersion(),
                item.isForceUpdateEnabled(),
                item.isGrayReleaseEnabled(),
                item.getTrafficPercentage() == null ? 100 : item.getTrafficPercentage(),
                item.getNote(),
                item.getUpdatedBy(),
                item.getUpdatedAt()
        );
    }

    private AndroidFeatureToggleDto toFeatureToggleDto(AndroidFeatureToggle item) {
        return new AndroidFeatureToggleDto(
                item.getId(),
                item.getFeatureKey(),
                item.getFeatureName(),
                item.getStatus(),
                item.getScope(),
                item.getOwner(),
                item.getNote(),
                item.getUpdatedBy(),
                item.getUpdatedAt()
        );
    }

    private AndroidAbExperimentDto toAbExperimentDto(AndroidAbExperiment item) {
        return new AndroidAbExperimentDto(
                item.getId(),
                item.getExperimentKey(),
                item.getExperimentName(),
                item.getStatus(),
                item.getLayerKey(),
                item.getOwner(),
                item.getChannelCode(),
                item.getMinAppVersion(),
                item.getMaxAppVersion(),
                item.getTrafficPercentage() == null ? 10 : item.getTrafficPercentage(),
                item.getVariantAName(),
                item.getVariantAPercentage() == null ? 50 : item.getVariantAPercentage(),
                item.getVariantBName(),
                item.getVariantBPercentage() == null ? 50 : item.getVariantBPercentage(),
                item.getHypothesis(),
                item.getSuccessMetric(),
                item.getNote(),
                item.getUpdatedBy(),
                item.getUpdatedAt()
        );
    }

    private AndroidGrayReleasePlanDto toGrayReleasePlanDto(AndroidGrayReleasePlan item) {
        return new AndroidGrayReleasePlanDto(
                item.getId(),
                item.getPlanCode(),
                item.getPlanName(),
                item.getStatus(),
                item.getChannelCode(),
                item.getMinAppVersion(),
                item.getMaxAppVersion(),
                item.getOverallTrafficPercentage() == null ? 10 : item.getOverallTrafficPercentage(),
                item.getNewUserPercentage() == null ? 50 : item.getNewUserPercentage(),
                item.getReturningUserPercentage() == null ? 50 : item.getReturningUserPercentage(),
                item.getWhitelistPercentage() == null ? 0 : item.getWhitelistPercentage(),
                item.getRegionCode(),
                item.getDeviceTier(),
                item.getFallbackPolicy(),
                item.getNote(),
                item.getUpdatedBy(),
                item.getUpdatedAt()
        );
    }

    private AndroidCompatibilityBlacklistDto toCompatibilityBlacklistDto(AndroidCompatibilityBlacklist item) {
        return new AndroidCompatibilityBlacklistDto(
                item.getId(),
                item.getRuleCode(),
                item.getStatus(),
                item.getTargetType(),
                item.getTargetValue(),
                item.getMinAppVersion(),
                item.getMaxAppVersion(),
                item.getMinSdkInt(),
                item.getMaxSdkInt(),
                item.getReason(),
                item.getNote(),
                item.getUpdatedBy(),
                item.getUpdatedAt()
        );
    }

    private AndroidPageCircuitBreakerDto toPageCircuitBreakerDto(AndroidPageCircuitBreaker item) {
        return new AndroidPageCircuitBreakerDto(
                item.getId(),
                item.getPageKey(),
                item.getPageName(),
                item.getStatus(),
                item.getAudienceScope(),
                item.getChannelCode(),
                item.getMinAppVersion(),
                item.getMaxAppVersion(),
                item.getDegradeMode(),
                item.getReason(),
                item.getNote(),
                item.getUpdatedBy(),
                item.getUpdatedAt()
        );
    }

    private List<AndroidHostCapability> hostCapabilities() {
        return List.of(
                capability("android.webview.lockdown", "WebView Sandbox", "security", "implemented", "GameRuntimeActivity", "JavaScript enabled with file/content access disabled and mixed content blocked by default"),
                capability("android.asset.loader", "App Asset Loader", "delivery", "implemented", "GameRuntimeActivity", "Loads game bundles from app internal storage via appassets domain"),
                capability("android.bridge.async", "Async JS Bridge", "bridge", "implemented", "NexusBridge", "AndroidApp bridge handles async wx APIs and callbacks"),
                capability("android.bridge.sync", "Sync JS Bridge", "bridge", "implemented", "NexusSyncBridge", "AndroidAppSync handles sync bridge invocation for supported APIs"),
                capability("android.runtime.update", "Runtime Update Channel", "delivery", "implemented", "UpdateApi/GameManager", "Supports update check and apply flow before entering runtime"),
                capability("android.runtime.overlay", "Runtime Capsule Menu", "runtime", "implemented", "GameRuntimeActivity", "Native runtime overlay supports back, share, favorite and refresh actions"),
                capability("android.native.network", "Native Network Proxy", "bridge", "implemented", "RequestApi", "wx.request is proxied through native OkHttp"),
                capability("android.local.storage", "Local Storage", "bridge", "implemented", "StorageApi", "wx storage APIs are backed by SharedPreferences"),
                capability("android.media.integration", "Media & Clipboard", "device", "partial", "ImageApi/ClipboardApi", "Image selection and save APIs are mock-oriented, clipboard APIs are available")
        );
    }

    private List<AndroidBridgeApiItem> bridgeApis() {
        return List.of(
                api("wx.login", "implemented", false, "LoginApi", "returns runtime session code derived from auth state"),
                api("wx.request", "implemented", false, "RequestApi", "native http proxy backed by OkHttp"),
                api("wx.getSystemInfoSync", "implemented", true, "SystemInfoApi", "returns runtime metrics and safe-area data"),
                api("wx.getMenuButtonBoundingClientRect", "implemented", true, "SystemInfoApi", "returns runtime capsule coordinates"),
                api("wx.update.check", "implemented", false, "UpdateApi", "checks cached bundle and remote version state"),
                api("wx.update.apply", "implemented", false, "UpdateApi", "applies prepared bundle update and restarts runtime"),
                api("wx.setStorage", "implemented", false, "StorageApi", "shared preferences persistence"),
                api("wx.getStorage", "implemented", false, "StorageApi", "shared preferences persistence"),
                api("wx.removeStorage", "implemented", false, "StorageApi", "shared preferences persistence"),
                api("wx.clearStorage", "implemented", false, "StorageApi", "shared preferences persistence"),
                api("wx.getUserInfo", "implemented", false, "UserInfoApi", "returns profile from backend session when available"),
                api("wx.shareAppMessage", "implemented", false, "ShareApi", "delegates to native share sheet"),
                api("wx.showToast", "implemented", false, "ToastApi", "native toast prompt"),
                api("wx.showModal", "implemented", false, "ModalApi", "renders native modal and returns user decision"),
                api("wx.downloadFile", "implemented", false, "FileApi", "downloads file via OkHttp into app cache"),
                api("wx.uploadFile", "implemented", false, "FileApi", "uploads local file using multipart form"),
                api("wx.getNetworkType", "implemented", false, "NetworkApi", "reads active transport from ConnectivityManager"),
                api("wx.chooseImage", "implemented", false, "ImageApi", "returns latest media items from device gallery query"),
                api("wx.previewImage", "implemented", false, "ImageApi", "opens native image viewer via ACTION_VIEW"),
                api("wx.getImageInfo", "implemented", false, "ImageApi", "reads dimensions/type from local or remote image source"),
                api("wx.saveImageToPhotosAlbum", "implemented", false, "ImageApi", "copies image into MediaStore album"),
                api("wx.setClipboardData", "implemented", false, "ClipboardApi", "system clipboard write"),
                api("wx.getClipboardData", "implemented", false, "ClipboardApi", "system clipboard read"),
                api("wx.vibrateShort", "implemented", false, "VibrateApi", "native vibration"),
                api("wx.vibrateLong", "implemented", false, "VibrateApi", "native vibration")
        );
    }

    private List<AndroidGameAssetRow> gameAssets() {
        return gameRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(game -> new AndroidGameAssetRow(
                        game.getId(),
                        game.getAppId(),
                        game.getName(),
                        game.getVersion(),
                        game.getStatus() == null ? "UNKNOWN" : game.getStatus().name(),
                        game.getDownloadUrl(),
                        game.getMd5(),
                        game.getStatus() == Game.GameStatus.APPROVED && game.getDownloadUrl() != null && !game.getDownloadUrl().isBlank(),
                        game.getUpdatedAt()
                ))
                .collect(Collectors.toList());
    }

    private AndroidBridgeApiItem api(String name, String status, boolean sync, String module, String notes) {
        return new AndroidBridgeApiItem(name, status, sync, module, notes);
    }

    private AndroidHostCapability capability(String key, String name, String category, String status, String sourceModule, String summary) {
        return new AndroidHostCapability(key, name, category, status, sourceModule, summary);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean boolOrDefault(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private int intOrDefault(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private String normalizeCode(String value, int maxLength) {
        String trimmed = trim(value, maxLength);
        if (trimmed == null) {
            return null;
        }
        String normalized = trimmed.toUpperCase(Locale.ROOT).replace('-', '_');
        return normalized.matches("^[A-Z0-9_]+$") ? normalized : null;
    }

    private String normalizeEnum(String value, Set<String> allowed, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return allowed.contains(normalized) ? normalized : fallback;
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }
}
