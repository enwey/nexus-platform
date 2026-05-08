package com.nexus.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.dto.ErrorCodes;
import com.nexus.platform.dto.GameOpsDtos.GameMediaAssetDeleteRequest;
import com.nexus.platform.dto.GameOpsDtos.GameMediaAssetResponse;
import com.nexus.platform.dto.GameOpsDtos.GameMediaAssetUpsertRequest;
import com.nexus.platform.dto.GameOpsDtos.GameOpsProfileResponse;
import com.nexus.platform.dto.GameOpsDtos.GameOpsProfileUpdateRequest;
import com.nexus.platform.dto.GameOpsDtos.RuntimeProfileResponse;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.GameMediaAsset;
import com.nexus.platform.entity.GameOpsProfile;
import com.nexus.platform.entity.GameVersion;
import com.nexus.platform.entity.User;
import com.nexus.platform.exception.PlatformException;
import com.nexus.platform.repository.GameMediaAssetRepository;
import com.nexus.platform.repository.GameOpsProfileRepository;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.GameVersionRepository;
import com.nexus.platform.repository.UserGameEngagementRepository;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameOpsProfileService {
    private static final Set<String> ASSET_GROUPS = Set.of("LIBRARY", "OPS");
    private static final Set<String> ASSET_ROLES = Set.of("SCREENSHOT", "BANNER", "LOGO", "ICON", "POSTER", "COVER", "SHARE", "DETAIL");
    private static final Set<String> MEDIA_TYPES = Set.of("IMAGE", "VIDEO");
    private static final Set<String> ASSET_STATUSES = Set.of("DRAFT", "ACTIVE", "ARCHIVED");
    private static final Set<String> OPERATIONS_STATUSES = Set.of("DRAFT", "READY", "PUBLISHED", "ARCHIVED");

    private final GameRepository gameRepository;
    private final GameOpsProfileRepository gameOpsProfileRepository;
    private final GameVersionRepository gameVersionRepository;
    private final UserGameEngagementRepository userGameEngagementRepository;
    private final GameMediaAssetRepository gameMediaAssetRepository;
    private final AuditLogService auditLogService;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public Result<GameOpsProfileResponse> getProfile(Long gameId) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            return Result.error("Game not found");
        }
        return Result.success(toResponse(gameId, gameOpsProfileRepository.findByGameId(gameId).orElse(null)));
    }

    public Result<GameOpsProfileResponse> updateProfile(Long gameId, GameOpsProfileUpdateRequest request, User currentUser) {
        return updateProfile(gameId, request, currentUser, "/admin/ops/game-profile/" + gameId);
    }

    public Result<GameOpsProfileResponse> updateProfile(Long gameId, GameOpsProfileUpdateRequest request, User currentUser, String requestUri) {
        if (request == null) {
            auditLogService.logOpsAudit("GAME_OPS_PROFILE_UPDATE", currentUser, gameId, "game:" + gameId, false, "Request body is required", requestUri);
            return Result.error("Request body is required");
        }
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            auditLogService.logOpsAudit("GAME_OPS_PROFILE_UPDATE", currentUser, gameId, "game:" + gameId, false, "Game not found", requestUri);
            return Result.error("Game not found");
        }

        String validation = validateProfileRequest(request);
        if (validation != null) {
            auditLogService.logOpsAudit("GAME_OPS_PROFILE_UPDATE", currentUser, game.getId(), game.getAppId(), false, validation, requestUri);
            return Result.error(validation);
        }

        GameOpsProfile profile = gameOpsProfileRepository.findByGameId(gameId).orElseGet(() -> {
            GameOpsProfile created = new GameOpsProfile();
            created.setGameId(gameId);
            return created;
        });
        applyProfile(profile, request, currentUser);
        GameOpsProfile saved = gameOpsProfileRepository.save(profile);
        auditLogService.logOpsAudit("GAME_OPS_PROFILE_UPDATE", currentUser, game.getId(), game.getAppId(), true, "Updated game operations profile", requestUri);
        return Result.success(toResponse(gameId, saved));
    }

    public Result<GameOpsProfileResponse> getDeveloperProfile(Long developerId, Long gameId, User currentUser) {
        Game game = requireDeveloperGame(developerId, gameId, currentUser);
        return Result.success(toResponse(game.getId(), gameOpsProfileRepository.findByGameId(gameId).orElse(null)));
    }

    @Transactional
    public Result<GameOpsProfileResponse> updateDeveloperProfile(Long developerId, Long gameId, GameOpsProfileUpdateRequest request, User currentUser, String requestUri) {
        Game game = requireDeveloperGame(developerId, gameId, currentUser);
        if (request == null) {
            auditLogService.logGameAudit("DEV_GAME_OPS_PROFILE_UPDATE", currentUser, game, false, "Request body is required", requestUri);
            return Result.error("Request body is required");
        }
        String validation = validateProfileRequest(request);
        if (validation != null) {
            auditLogService.logGameAudit("DEV_GAME_OPS_PROFILE_UPDATE", currentUser, game, false, validation, requestUri);
            return Result.error(validation);
        }
        GameOpsProfile profile = gameOpsProfileRepository.findByGameId(gameId).orElseGet(() -> {
            GameOpsProfile created = new GameOpsProfile();
            created.setGameId(gameId);
            return created;
        });
        applyProfile(profile, request, currentUser);
        GameOpsProfile saved = gameOpsProfileRepository.save(profile);
        auditLogService.logGameAudit("DEV_GAME_OPS_PROFILE_UPDATE", currentUser, game, true, "Updated developer operations profile", requestUri);
        return Result.success(toResponse(gameId, saved));
    }

    public Result<List<GameMediaAssetResponse>> listDeveloperAssets(Long developerId, Long gameId, String assetGroup, User currentUser) {
        Game game = requireDeveloperGame(developerId, gameId, currentUser);
        String normalizedGroup = normalizeEnum(assetGroup, ASSET_GROUPS);
        if (assetGroup != null && !assetGroup.isBlank() && normalizedGroup == null) {
            return Result.error("Invalid asset group");
        }
        List<GameMediaAsset> assets = normalizedGroup == null
                ? gameMediaAssetRepository.findByGameIdOrderByAssetGroupAscSortOrderAscPrimaryDesc(game.getId())
                : gameMediaAssetRepository.findByGameIdAndAssetGroupOrderBySortOrderAscPrimaryDesc(game.getId(), normalizedGroup);
        return Result.success(assets.stream().map(this::toAssetResponse).toList());
    }

    @Transactional
    public Result<GameMediaAssetResponse> upsertDeveloperAsset(
            Long developerId,
            Long gameId,
            GameMediaAssetUpsertRequest request,
            User currentUser,
            String requestUri
    ) {
        Game game = requireDeveloperGame(developerId, gameId, currentUser);
        AssetValidation validation = validateAssetRequest(request);
        if (!validation.success()) {
            auditLogService.logGameAudit("DEV_GAME_ASSET_UPSERT", currentUser, game, false, validation.message(), requestUri);
            return Result.error(validation.message());
        }

        GameMediaAsset asset;
        boolean created;
        if (request != null && request.assetId() != null) {
            asset = gameMediaAssetRepository.findByIdAndGameId(request.assetId(), game.getId()).orElse(null);
            if (asset == null) {
                auditLogService.logGameAudit("DEV_GAME_ASSET_UPSERT", currentUser, game, false, "Asset not found", requestUri);
                return Result.error("Asset not found");
            }
            created = false;
        } else {
            asset = new GameMediaAsset();
            asset.setGameId(game.getId());
            created = true;
        }

        applyAsset(asset, request, validation);
        GameMediaAsset saved = gameMediaAssetRepository.save(asset);
        if (Boolean.TRUE.equals(saved.getPrimary())) {
            normalizePrimaryAsset(saved);
        }
        auditLogService.logGameAudit(
                created ? "DEV_GAME_ASSET_CREATE" : "DEV_GAME_ASSET_UPDATE",
                currentUser,
                game,
                true,
                saved.getAssetGroup() + ":" + saved.getAssetRole() + ":" + saved.getUrl(),
                requestUri
        );
        return Result.success(toAssetResponse(saved));
    }

    @Transactional
    public Result<Boolean> deleteDeveloperAsset(
            Long developerId,
            Long gameId,
            Long assetId,
            GameMediaAssetDeleteRequest request,
            User currentUser,
            String requestUri
    ) {
        Game game = requireDeveloperGame(developerId, gameId, currentUser);
        String confirmText = request == null ? null : request.confirmText();
        if (confirmText == null || !"DELETE".equalsIgnoreCase(confirmText.trim())) {
            auditLogService.logGameAudit("DEV_GAME_ASSET_DELETE", currentUser, game, false, "Confirmation text mismatch", requestUri);
            return Result.error("Confirmation text mismatch");
        }
        GameMediaAsset asset = gameMediaAssetRepository.findByIdAndGameId(assetId, game.getId()).orElse(null);
        if (asset == null) {
            auditLogService.logGameAudit("DEV_GAME_ASSET_DELETE", currentUser, game, false, "Asset not found", requestUri);
            return Result.error("Asset not found");
        }
        gameMediaAssetRepository.delete(asset);
        auditLogService.logGameAudit("DEV_GAME_ASSET_DELETE", currentUser, game, true, asset.getAssetGroup() + ":" + asset.getAssetRole(), requestUri);
        return Result.success(Boolean.TRUE);
    }

    public Result<RuntimeProfileResponse> getRuntimeProfile(String appId) {
        Game game = gameRepository.findByAppId(appId);
        if (game == null || game.getStatus() != Game.GameStatus.APPROVED) {
            return Result.error("Game not found");
        }

        Optional<GameOpsProfile> profileOpt = gameOpsProfileRepository.findByGameId(game.getId());
        GameOpsProfile profile = profileOpt.orElse(null);
        String studio = profile == null || profile.getStudioName() == null || profile.getStudioName().isBlank()
                ? "Nexus Studio"
                : profile.getStudioName();
        String players = profile == null || profile.getPlayerCountText() == null || profile.getPlayerCountText().isBlank()
                ? "2.4M+"
                : profile.getPlayerCountText();
        String shareTitle = profile == null || profile.getShareTitle() == null || profile.getShareTitle().isBlank()
                ? game.getName()
                : profile.getShareTitle();
        String shareSubtitle = profile == null || profile.getShareSubtitle() == null || profile.getShareSubtitle().isBlank()
                ? game.getDescription()
                : profile.getShareSubtitle();
        Long packageSizeBytes = resolvePackageSizeBytes(game);
        CategoryRankSnapshot categoryRank = resolveCategoryRank(game);

        RuntimeProfileResponse response = new RuntimeProfileResponse(
                game.getAppId(),
                game.getName(),
                studio,
                players,
                packageSizeBytes,
                categoryRank.playerCount(),
                categoryRank.rank(),
                categoryRank.categoryName(),
                profile == null ? null : profile.getRuntimeBannerUrl(),
                profile == null ? null : profile.getRuntimeLogoUrl(),
                shareTitle,
                shareSubtitle,
                profile == null ? null : profile.getShareImageUrl()
        );
        return Result.success(response);
    }

    private void applyProfile(GameOpsProfile profile, GameOpsProfileUpdateRequest request, User currentUser) {
        profile.setStudioName(trimToNull(request.studioName(), 128));
        profile.setPlayerCountText(trimToNull(request.playerCountText(), 128));
        profile.setRuntimeBannerUrl(trimToNull(request.runtimeBannerUrl(), 512));
        profile.setRuntimeLogoUrl(trimToNull(request.runtimeLogoUrl(), 512));
        profile.setShareTitle(trimToNull(request.shareTitle(), 128));
        profile.setShareSubtitle(trimToNull(request.shareSubtitle(), 256));
        profile.setShareImageUrl(trimToNull(request.shareImageUrl(), 512));
        profile.setDiscoverCardCoverUrl(trimToNull(request.discoverCardCoverUrl(), 512));
        profile.setDiscoverCardLogoUrl(trimToNull(request.discoverCardLogoUrl(), 512));
        profile.setMarketingTagline(trimToNull(request.marketingTagline(), 160));
        profile.setMarketingSummary(trimToNull(request.marketingSummary(), 500));
        profile.setFeatureHighlightsJson(writeFeatureHighlights(request.featureHighlights()));
        profile.setTargetAudience(trimToNull(request.targetAudience(), 120));
        profile.setSupportEmail(trimToNull(request.supportEmail(), 160));
        profile.setSupportUrl(trimToNull(request.supportUrl(), 512));
        profile.setCommunityUrl(trimToNull(request.communityUrl(), 512));
        profile.setComplianceNote(trimToNull(request.complianceNote(), 500));
        profile.setOperationsStatus(defaultIfBlank(normalizeEnum(request.operationsStatus(), OPERATIONS_STATUSES), "DRAFT"));
        profile.setUpdatedBy(currentUser == null ? null : currentUser.getId());
    }

    private void applyAsset(GameMediaAsset asset, GameMediaAssetUpsertRequest request, AssetValidation validation) {
        asset.setVersionId(request == null ? null : request.versionId());
        asset.setAssetGroup(validation.assetGroup());
        asset.setAssetRole(validation.assetRole());
        asset.setMediaType(validation.mediaType());
        asset.setAssetStatus(validation.assetStatus());
        asset.setTitle(validation.title());
        asset.setDescription(validation.description());
        asset.setUrl(validation.url());
        asset.setActionTitle(validation.actionTitle());
        asset.setActionUrl(validation.actionUrl());
        asset.setWidth(validation.width());
        asset.setHeight(validation.height());
        asset.setSizeBytes(validation.sizeBytes());
        asset.setLocale(validation.locale());
        asset.setSortOrder(validation.sortOrder());
        asset.setPrimary(validation.primary());
    }

    private GameOpsProfileResponse toResponse(Long gameId, GameOpsProfile profile) {
        if (profile == null) {
            return new GameOpsProfileResponse(
                    gameId, null, null, null, null, null, null, null, null, null,
                    null, null, List.of(), null, null, null, null, null, "DRAFT"
            );
        }
        return new GameOpsProfileResponse(
                gameId,
                profile.getStudioName(),
                profile.getPlayerCountText(),
                profile.getRuntimeBannerUrl(),
                profile.getRuntimeLogoUrl(),
                profile.getShareTitle(),
                profile.getShareSubtitle(),
                profile.getShareImageUrl(),
                profile.getDiscoverCardCoverUrl(),
                profile.getDiscoverCardLogoUrl(),
                profile.getMarketingTagline(),
                profile.getMarketingSummary(),
                readFeatureHighlights(profile.getFeatureHighlightsJson()),
                profile.getTargetAudience(),
                profile.getSupportEmail(),
                profile.getSupportUrl(),
                profile.getCommunityUrl(),
                profile.getComplianceNote(),
                defaultIfBlank(profile.getOperationsStatus(), "DRAFT")
        );
    }

    private GameMediaAssetResponse toAssetResponse(GameMediaAsset asset) {
        return new GameMediaAssetResponse(
                asset.getId(),
                asset.getGameId(),
                asset.getVersionId(),
                asset.getAssetGroup(),
                asset.getAssetRole(),
                asset.getMediaType(),
                asset.getAssetStatus(),
                asset.getTitle(),
                asset.getDescription(),
                asset.getUrl(),
                asset.getActionTitle(),
                asset.getActionUrl(),
                asset.getWidth(),
                asset.getHeight(),
                asset.getSizeBytes(),
                asset.getLocale(),
                asset.getSortOrder(),
                asset.getPrimary(),
                asset.getCreatedAt(),
                asset.getUpdatedAt()
        );
    }

    private String validateProfileRequest(GameOpsProfileUpdateRequest request) {
        if (!isLengthValid(request.studioName(), 128)) return "Studio name is too long";
        if (!isLengthValid(request.playerCountText(), 128)) return "Player count text is too long";
        if (!isLengthValid(request.shareTitle(), 128)) return "Share title is too long";
        if (!isLengthValid(request.shareSubtitle(), 256)) return "Share subtitle is too long";
        if (!isLengthValid(request.marketingTagline(), 160)) return "Marketing tagline is too long";
        if (!isLengthValid(request.marketingSummary(), 500)) return "Marketing summary is too long";
        if (!isLengthValid(request.targetAudience(), 120)) return "Target audience is too long";
        if (!isLengthValid(request.complianceNote(), 500)) return "Compliance note is too long";
        if (!isValidUrl(request.runtimeBannerUrl())) return "Invalid runtime banner URL";
        if (!isValidUrl(request.runtimeLogoUrl())) return "Invalid runtime logo URL";
        if (!isValidUrl(request.shareImageUrl())) return "Invalid share image URL";
        if (!isValidUrl(request.discoverCardCoverUrl())) return "Invalid discover card cover URL";
        if (!isValidUrl(request.discoverCardLogoUrl())) return "Invalid discover card logo URL";
        if (!isValidUrl(request.supportUrl())) return "Invalid support URL";
        if (!isValidUrl(request.communityUrl())) return "Invalid community URL";
        if (request.supportEmail() != null && !request.supportEmail().isBlank() && !request.supportEmail().contains("@")) {
            return "Invalid support email";
        }
        if (request.operationsStatus() != null && normalizeEnum(request.operationsStatus(), OPERATIONS_STATUSES) == null) {
            return "Invalid operations status";
        }
        if (request.featureHighlights() != null && request.featureHighlights().size() > 8) {
            return "Too many feature highlights";
        }
        if (request.featureHighlights() != null) {
            for (String item : request.featureHighlights()) {
                String normalized = trimToNull(item, 80);
                if (normalized == null || normalized.length() < 2) {
                    return "Feature highlight must be at least 2 characters";
                }
            }
        }
        return null;
    }

    private AssetValidation validateAssetRequest(GameMediaAssetUpsertRequest request) {
        if (request == null) {
            return AssetValidation.error("Request body is required");
        }
        String assetGroup = normalizeEnum(request.assetGroup(), ASSET_GROUPS);
        if (assetGroup == null) {
            return AssetValidation.error("Invalid asset group");
        }
        String assetRole = normalizeEnum(request.assetRole(), ASSET_ROLES);
        if (assetRole == null) {
            return AssetValidation.error("Invalid asset role");
        }
        String mediaType = normalizeEnum(request.mediaType(), MEDIA_TYPES);
        if (mediaType == null) {
            return AssetValidation.error("Invalid media type");
        }
        String assetStatus = defaultIfBlank(normalizeEnum(request.assetStatus(), ASSET_STATUSES), "DRAFT");
        String url = trimToNull(request.url(), 512);
        if (!isValidUrl(url)) {
            return AssetValidation.error("Invalid asset URL");
        }
        String actionUrl = trimToNull(request.actionUrl(), 512);
        if (!isValidUrl(actionUrl)) {
            return AssetValidation.error("Invalid action URL");
        }
        if (request.width() != null && request.width() < 0) return AssetValidation.error("Invalid asset width");
        if (request.height() != null && request.height() < 0) return AssetValidation.error("Invalid asset height");
        if (request.sizeBytes() != null && request.sizeBytes() < 0) return AssetValidation.error("Invalid asset size");
        return AssetValidation.success(
                assetGroup,
                assetRole,
                mediaType,
                assetStatus,
                trimToNull(request.title(), 80),
                trimToNull(request.description(), 500),
                url,
                trimToNull(request.actionTitle(), 40),
                actionUrl,
                request.width(),
                request.height(),
                request.sizeBytes(),
                defaultIfBlank(trimToNull(request.locale(), 16), "zh-CN"),
                request.sortOrder() == null ? 0 : Math.max(0, Math.min(request.sortOrder(), 999)),
                Boolean.TRUE.equals(request.primary())
        );
    }

    private void normalizePrimaryAsset(GameMediaAsset saved) {
        List<GameMediaAsset> siblings = gameMediaAssetRepository.findByGameIdAndAssetGroupAndAssetRole(
                saved.getGameId(),
                saved.getAssetGroup(),
                saved.getAssetRole()
        );
        boolean changed = false;
        for (GameMediaAsset sibling : siblings) {
            if (saved.getId().equals(sibling.getId())) {
                continue;
            }
            if (Boolean.TRUE.equals(sibling.getPrimary())) {
                sibling.setPrimary(false);
                changed = true;
            }
        }
        if (changed) {
            gameMediaAssetRepository.saveAll(siblings);
        }
    }

    private String trimToNull(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private boolean isLengthValid(String value, int maxLength) {
        return value == null || value.trim().length() <= maxLength;
    }

    private boolean isValidUrl(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private String normalizeEnum(String value, Set<String> allowed) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return allowed.contains(normalized) ? normalized : null;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String writeFeatureHighlights(List<String> highlights) {
        if (highlights == null || highlights.isEmpty()) {
            return null;
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String item : highlights) {
            String value = trimToNull(item, 80);
            if (value != null) {
                normalized.add(value);
            }
            if (normalized.size() >= 8) {
                break;
            }
        }
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(normalized.stream().toList());
        } catch (Exception exception) {
            return null;
        }
    }

    private List<String> readFeatureHighlights(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            return List.of();
        }
    }

    private Game requireDeveloperGame(Long developerId, Long gameId, User currentUser) {
        if (currentUser == null) {
            throw new PlatformException(ErrorCodes.COMMON_UNAUTHORIZED, "Missing valid login token", HttpStatus.UNAUTHORIZED);
        }
        if (developerId == null || !developerId.equals(currentUser.getId())) {
            throw new PlatformException(ErrorCodes.COMMON_FORBIDDEN, "Permission denied", HttpStatus.FORBIDDEN);
        }
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            throw new PlatformException(ErrorCodes.COMMON_NOT_FOUND, "Game not found", HttpStatus.NOT_FOUND);
        }
        if (game.getDeveloperId() == null || !game.getDeveloperId().equals(currentUser.getId())) {
            throw new PlatformException(ErrorCodes.COMMON_FORBIDDEN, "Permission denied", HttpStatus.FORBIDDEN);
        }
        return game;
    }

    private Long resolvePackageSizeBytes(Game game) {
        String storageKey = resolvePackageStorageKey(game);
        if (storageKey == null) {
            return null;
        }
        try {
            StatObjectResponse object = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageKey)
                            .build()
            );
            return object.size();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolvePackageStorageKey(Game game) {
        GameVersion latestApproved = gameVersionRepository
                .findTopByGameIdAndStatusOrderByCreatedAtDesc(game.getId(), GameVersion.VersionStatus.APPROVED)
                .orElse(null);
        if (latestApproved != null && latestApproved.getStorageKey() != null && !latestApproved.getStorageKey().isBlank()) {
            return latestApproved.getStorageKey();
        }
        if (game.getStorageKey() != null && !game.getStorageKey().isBlank()) {
            return game.getStorageKey();
        }
        return null;
    }

    private CategoryRankSnapshot resolveCategoryRank(Game game) {
        String categoryName = trimToNull(game.getCategory(), 32);
        if (categoryName == null) {
            return new CategoryRankSnapshot(null, null, null);
        }

        List<Game> categoryGames = gameRepository.findByStatusAndCategoryIgnoreCase(Game.GameStatus.APPROVED, categoryName);
        if (categoryGames.isEmpty()) {
            return new CategoryRankSnapshot(categoryName, null, null);
        }

        List<String> appIds = categoryGames.stream()
                .map(Game::getAppId)
                .filter(appId -> appId != null && !appId.isBlank())
                .toList();
        if (appIds.isEmpty()) {
            return new CategoryRankSnapshot(categoryName, null, null);
        }

        Map<String, Long> playerCountByAppId = new HashMap<>();
        for (UserGameEngagementRepository.GamePlayerCountAggregate aggregate : userGameEngagementRepository.aggregatePlayerCountByAppIds(appIds)) {
            playerCountByAppId.put(aggregate.getAppId(), Optional.ofNullable(aggregate.getPlayerCount()).orElse(0L));
        }

        List<GamePlayerRankRow> rows = categoryGames.stream()
                .map(categoryGame -> new GamePlayerRankRow(
                        categoryGame.getAppId(),
                        Optional.ofNullable(playerCountByAppId.get(categoryGame.getAppId())).orElse(0L)
                ))
                .sorted(Comparator.comparingLong(GamePlayerRankRow::playerCount).reversed().thenComparing(GamePlayerRankRow::appId, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Long currentPlayerCount = null;
        Integer currentRank = null;
        for (int index = 0; index < rows.size(); index++) {
            GamePlayerRankRow row = rows.get(index);
            if (row.appId().equalsIgnoreCase(game.getAppId())) {
                currentPlayerCount = row.playerCount();
                currentRank = index + 1;
                break;
            }
        }
        return new CategoryRankSnapshot(categoryName, currentPlayerCount, currentRank);
    }

    private record GamePlayerRankRow(String appId, long playerCount) {}

    private record CategoryRankSnapshot(String categoryName, Long playerCount, Integer rank) {}

    private record AssetValidation(
            boolean success,
            String message,
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
    ) {
        private static AssetValidation error(String message) {
            return new AssetValidation(false, message, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        private static AssetValidation success(
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
        ) {
            return new AssetValidation(true, null, assetGroup, assetRole, mediaType, assetStatus, title, description, url, actionTitle, actionUrl, width, height, sizeBytes, locale, sortOrder, primary);
        }
    }
}
