package com.nexus.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.dto.GameOpsDtos.GameMediaAssetDeleteRequest;
import com.nexus.platform.dto.GameOpsDtos.GameMediaAssetResponse;
import com.nexus.platform.dto.GameOpsDtos.GameMediaAssetUpsertRequest;
import com.nexus.platform.dto.GameOpsDtos.GameOpsProfileResponse;
import com.nexus.platform.dto.GameOpsDtos.GameOpsProfileUpdateRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.GameMediaAsset;
import com.nexus.platform.entity.GameOpsProfile;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.GameMediaAssetRepository;
import com.nexus.platform.repository.GameOpsProfileRepository;
import com.nexus.platform.repository.GameRepository;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeveloperGameAssetService {
    private static final Set<String> OPERATIONS_STATUSES = Set.of("DRAFT", "READY", "PUBLISHED", "PAUSED");
    private static final Set<String> ASSET_GROUPS = Set.of("LIBRARY", "STORE", "SHARE");
    private static final Set<String> ASSET_ROLES = Set.of("SCREENSHOT", "BANNER", "ICON", "POSTER");
    private static final Set<String> MEDIA_TYPES = Set.of("IMAGE", "VIDEO");
    private static final Set<String> ASSET_STATUSES = Set.of("DRAFT", "ACTIVE", "ARCHIVED");
    private static final String DELETE_CONFIRM_TEXT = "DELETE";

    private final GameRepository gameRepository;
    private final GameOpsProfileRepository gameOpsProfileRepository;
    private final GameMediaAssetRepository gameMediaAssetRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public Result<GameOpsProfileResponse> getProfile(Long gameId, User currentUser) {
        Game game = getOwnedGame(gameId, currentUser);
        if (game == null) {
            return Result.error("Game not found");
        }
        return Result.success(toProfileResponse(gameId, gameOpsProfileRepository.findByGameId(gameId).orElse(null)));
    }

    @Transactional
    public Result<GameOpsProfileResponse> updateProfile(Long gameId, GameOpsProfileUpdateRequest request, User currentUser, String requestUri) {
        Game game = getOwnedGame(gameId, currentUser);
        if (game == null) {
            auditLogService.logGameAudit("DEV_GAME_OPS_PROFILE_UPDATE", currentUser, null, false, "Game not found", requestUri);
            return Result.error("Game not found");
        }
        ValidationResult validation = validateProfile(request);
        if (!validation.success()) {
            auditLogService.logGameAudit("DEV_GAME_OPS_PROFILE_UPDATE", currentUser, game, false, validation.message(), requestUri);
            return Result.error(validation.message());
        }

        GameOpsProfile profile = gameOpsProfileRepository.findByGameId(gameId).orElseGet(() -> {
            GameOpsProfile created = new GameOpsProfile();
            created.setGameId(gameId);
            return created;
        });
        Map<String, Object> before = buildProfileSnapshot(profile);
        applyProfile(profile, validation, currentUser);
        GameOpsProfile saved = gameOpsProfileRepository.save(profile);
        auditLogService.logGameAudit(
                "DEV_GAME_OPS_PROFILE_UPDATE",
                currentUser,
                game,
                true,
                "Developer updated game operations profile",
                requestUri,
                "GAME_OPS_PROFILE",
                before,
                buildProfileSnapshot(saved)
        );
        return Result.success(toProfileResponse(gameId, saved));
    }

    public Result<List<GameMediaAssetResponse>> listMediaAssets(Long gameId, String assetGroup, User currentUser) {
        Game game = getOwnedGame(gameId, currentUser);
        if (game == null) {
            return Result.error("Game not found");
        }
        String normalizedGroup = normalizeEnum(assetGroup, ASSET_GROUPS);
        List<GameMediaAsset> rows = normalizedGroup == null
                ? gameMediaAssetRepository.findByGameIdOrderByAssetGroupAscSortOrderAscPrimaryDesc(gameId)
                : gameMediaAssetRepository.findByGameIdAndAssetGroupOrderBySortOrderAscPrimaryDesc(gameId, normalizedGroup);
        return Result.success(rows.stream().map(this::toMediaAssetResponse).toList());
    }

    @Transactional
    public Result<GameMediaAssetResponse> upsertMediaAsset(Long gameId, GameMediaAssetUpsertRequest request, User currentUser, String requestUri) {
        Game game = getOwnedGame(gameId, currentUser);
        if (game == null) {
            auditLogService.logGameAudit("DEV_GAME_MEDIA_UPSERT", currentUser, null, false, "Game not found", requestUri);
            return Result.error("Game not found");
        }
        MediaValidationResult validation = validateMediaRequest(request);
        if (!validation.success()) {
            auditLogService.logGameAudit("DEV_GAME_MEDIA_UPSERT", currentUser, game, false, validation.message(), requestUri);
            return Result.error(validation.message());
        }

        GameMediaAsset row = request != null && request.assetId() != null
                ? gameMediaAssetRepository.findByIdAndGameId(request.assetId(), gameId).orElse(null)
                : null;
        if (request != null && request.assetId() != null && row == null) {
            auditLogService.logGameAudit("DEV_GAME_MEDIA_UPSERT", currentUser, game, false, "Asset not found", requestUri);
            return Result.error("Asset not found");
        }
        if (row == null) {
            row = new GameMediaAsset();
            row.setGameId(gameId);
        }
        Map<String, Object> before = buildMediaSnapshot(row);
        applyMediaAsset(row, validation);
        if (Boolean.TRUE.equals(validation.primary())) {
            clearOtherPrimaryAssets(gameId, row.getId(), row.getAssetGroup(), row.getAssetRole());
        }
        GameMediaAsset saved = gameMediaAssetRepository.save(row);
        auditLogService.logGameAudit(
                "DEV_GAME_MEDIA_UPSERT",
                currentUser,
                game,
                true,
                saved.getAssetGroup() + ":" + saved.getAssetRole(),
                requestUri,
                "GAME_MEDIA_ASSET",
                before,
                buildMediaSnapshot(saved)
        );
        return Result.success(toMediaAssetResponse(saved));
    }

    @Transactional
    public Result<Void> deleteMediaAsset(Long gameId, Long assetId, GameMediaAssetDeleteRequest request, User currentUser, String requestUri) {
        Game game = getOwnedGame(gameId, currentUser);
        if (game == null) {
            auditLogService.logGameAudit("DEV_GAME_MEDIA_DELETE", currentUser, null, false, "Game not found", requestUri);
            return Result.error("Game not found");
        }
        GameMediaAsset row = gameMediaAssetRepository.findByIdAndGameId(assetId, gameId).orElse(null);
        if (row == null) {
            auditLogService.logGameAudit("DEV_GAME_MEDIA_DELETE", currentUser, game, false, "Asset not found", requestUri);
            return Result.error("Asset not found");
        }
        String confirmText = trim(request == null ? null : request.confirmText(), 32);
        if (!DELETE_CONFIRM_TEXT.equalsIgnoreCase(confirmText)) {
            auditLogService.logGameAudit("DEV_GAME_MEDIA_DELETE", currentUser, game, false, "Confirmation text mismatch", requestUri);
            return Result.error("Confirmation text mismatch");
        }
        Map<String, Object> before = buildMediaSnapshot(row);
        gameMediaAssetRepository.delete(row);
        auditLogService.logGameAudit(
                "DEV_GAME_MEDIA_DELETE",
                currentUser,
                game,
                true,
                row.getAssetGroup() + ":" + row.getAssetRole(),
                requestUri,
                "GAME_MEDIA_ASSET",
                before,
                Map.of()
        );
        return Result.success();
    }

    private Game getOwnedGame(Long gameId, User currentUser) {
        if (gameId == null || currentUser == null) {
            return null;
        }
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null || !currentUser.getId().equals(game.getDeveloperId())) {
            return null;
        }
        return game;
    }

    private ValidationResult validateProfile(GameOpsProfileUpdateRequest request) {
        if (request == null) {
            return ValidationResult.error("Request body is required");
        }
        List<String> featureHighlights = normalizeHighlights(request.featureHighlights());
        if (featureHighlights == null) {
            return ValidationResult.error("Feature highlights must contain 1-8 valid lines");
        }
        String supportEmail = trim(request.supportEmail(), 160);
        if (supportEmail != null && !supportEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            return ValidationResult.error("Invalid support email");
        }
        if (!allUrlsValid(
                request.runtimeBannerUrl(),
                request.runtimeLogoUrl(),
                request.shareImageUrl(),
                request.discoverCardCoverUrl(),
                request.discoverCardLogoUrl(),
                request.supportUrl(),
                request.communityUrl()
        )) {
            return ValidationResult.error("Invalid profile url");
        }
        String operationsStatus = normalizeEnum(request.operationsStatus(), OPERATIONS_STATUSES);
        if (operationsStatus == null) {
            operationsStatus = "DRAFT";
        }
        return ValidationResult.success(
                trim(request.studioName(), 128),
                trim(request.playerCountText(), 128),
                trim(request.runtimeBannerUrl(), 512),
                trim(request.runtimeLogoUrl(), 512),
                trim(request.shareTitle(), 128),
                trim(request.shareSubtitle(), 256),
                trim(request.shareImageUrl(), 512),
                trim(request.discoverCardCoverUrl(), 512),
                trim(request.discoverCardLogoUrl(), 512),
                trim(request.marketingTagline(), 160),
                trim(request.marketingSummary(), 500),
                featureHighlights,
                trim(request.targetAudience(), 120),
                supportEmail,
                trim(request.supportUrl(), 512),
                trim(request.communityUrl(), 512),
                trim(request.complianceNote(), 500),
                operationsStatus
        );
    }

    private MediaValidationResult validateMediaRequest(GameMediaAssetUpsertRequest request) {
        if (request == null) {
            return MediaValidationResult.error("Request body is required");
        }
        String assetGroup = normalizeEnum(request.assetGroup(), ASSET_GROUPS);
        String assetRole = normalizeEnum(request.assetRole(), ASSET_ROLES);
        String mediaType = normalizeEnum(request.mediaType(), MEDIA_TYPES);
        String assetStatus = normalizeEnum(request.assetStatus(), ASSET_STATUSES);
        if (assetGroup == null || assetRole == null || mediaType == null || assetStatus == null) {
            return MediaValidationResult.error("Invalid asset group, role, media type, or status");
        }
        String url = trim(request.url(), 512);
        if (!isValidUrl(url)) {
            return MediaValidationResult.error("Invalid asset url");
        }
        String actionUrl = trim(request.actionUrl(), 512);
        if (actionUrl != null && !isValidUrl(actionUrl)) {
            return MediaValidationResult.error("Invalid action url");
        }
        Integer sortOrder = request.sortOrder() == null ? 0 : Math.max(0, Math.min(999, request.sortOrder()));
        return MediaValidationResult.success(
                assetGroup,
                assetRole,
                mediaType,
                assetStatus,
                trim(request.title(), 80),
                trim(request.description(), 500),
                url,
                trim(request.actionTitle(), 40),
                actionUrl,
                request.width(),
                request.height(),
                request.sizeBytes(),
                trim(request.locale(), 16) == null ? "zh-CN" : trim(request.locale(), 16),
                sortOrder,
                request.primary() != null && request.primary()
        );
    }

    private void applyProfile(GameOpsProfile profile, ValidationResult validation, User currentUser) {
        profile.setStudioName(validation.studioName());
        profile.setPlayerCountText(validation.playerCountText());
        profile.setRuntimeBannerUrl(validation.runtimeBannerUrl());
        profile.setRuntimeLogoUrl(validation.runtimeLogoUrl());
        profile.setShareTitle(validation.shareTitle());
        profile.setShareSubtitle(validation.shareSubtitle());
        profile.setShareImageUrl(validation.shareImageUrl());
        profile.setDiscoverCardCoverUrl(validation.discoverCardCoverUrl());
        profile.setDiscoverCardLogoUrl(validation.discoverCardLogoUrl());
        profile.setMarketingTagline(validation.marketingTagline());
        profile.setMarketingSummary(validation.marketingSummary());
        profile.setFeatureHighlightsJson(writeHighlights(validation.featureHighlights()));
        profile.setTargetAudience(validation.targetAudience());
        profile.setSupportEmail(validation.supportEmail());
        profile.setSupportUrl(validation.supportUrl());
        profile.setCommunityUrl(validation.communityUrl());
        profile.setComplianceNote(validation.complianceNote());
        profile.setOperationsStatus(validation.operationsStatus());
        profile.setUpdatedBy(currentUser == null ? null : currentUser.getId());
    }

    private void applyMediaAsset(GameMediaAsset row, MediaValidationResult validation) {
        row.setAssetGroup(validation.assetGroup());
        row.setAssetRole(validation.assetRole());
        row.setMediaType(validation.mediaType());
        row.setAssetStatus(validation.assetStatus());
        row.setTitle(validation.title());
        row.setDescription(validation.description());
        row.setUrl(validation.url());
        row.setActionTitle(validation.actionTitle());
        row.setActionUrl(validation.actionUrl());
        row.setWidth(validation.width());
        row.setHeight(validation.height());
        row.setSizeBytes(validation.sizeBytes());
        row.setLocale(validation.locale());
        row.setSortOrder(validation.sortOrder());
        row.setPrimary(validation.primary());
    }

    private void clearOtherPrimaryAssets(Long gameId, Long currentAssetId, String assetGroup, String assetRole) {
        for (GameMediaAsset asset : gameMediaAssetRepository.findByGameIdAndAssetGroupAndAssetRole(gameId, assetGroup, assetRole)) {
            if (currentAssetId != null && currentAssetId.equals(asset.getId())) {
                continue;
            }
            if (Boolean.TRUE.equals(asset.getPrimary())) {
                asset.setPrimary(false);
                gameMediaAssetRepository.save(asset);
            }
        }
    }

    private GameOpsProfileResponse toProfileResponse(Long gameId, GameOpsProfile profile) {
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
                readHighlights(profile.getFeatureHighlightsJson()),
                profile.getTargetAudience(),
                profile.getSupportEmail(),
                profile.getSupportUrl(),
                profile.getCommunityUrl(),
                profile.getComplianceNote(),
                profile.getOperationsStatus()
        );
    }

    private GameMediaAssetResponse toMediaAssetResponse(GameMediaAsset row) {
        return new GameMediaAssetResponse(
                row.getId(),
                row.getGameId(),
                row.getVersionId(),
                row.getAssetGroup(),
                row.getAssetRole(),
                row.getMediaType(),
                row.getAssetStatus(),
                row.getTitle(),
                row.getDescription(),
                row.getUrl(),
                row.getActionTitle(),
                row.getActionUrl(),
                row.getWidth(),
                row.getHeight(),
                row.getSizeBytes(),
                row.getLocale(),
                row.getSortOrder(),
                row.getPrimary(),
                row.getCreatedAt(),
                row.getUpdatedAt()
        );
    }

    private Map<String, Object> buildProfileSnapshot(GameOpsProfile profile) {
        if (profile == null) {
            return Map.of();
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("studioName", value(profile.getStudioName()));
        snapshot.put("playerCountText", value(profile.getPlayerCountText()));
        snapshot.put("runtimeBannerUrl", value(profile.getRuntimeBannerUrl()));
        snapshot.put("runtimeLogoUrl", value(profile.getRuntimeLogoUrl()));
        snapshot.put("shareTitle", value(profile.getShareTitle()));
        snapshot.put("shareSubtitle", value(profile.getShareSubtitle()));
        snapshot.put("shareImageUrl", value(profile.getShareImageUrl()));
        snapshot.put("discoverCardCoverUrl", value(profile.getDiscoverCardCoverUrl()));
        snapshot.put("discoverCardLogoUrl", value(profile.getDiscoverCardLogoUrl()));
        snapshot.put("marketingTagline", value(profile.getMarketingTagline()));
        snapshot.put("marketingSummary", value(profile.getMarketingSummary()));
        snapshot.put("featureHighlights", readHighlights(profile.getFeatureHighlightsJson()));
        snapshot.put("targetAudience", value(profile.getTargetAudience()));
        snapshot.put("supportEmail", value(profile.getSupportEmail()));
        snapshot.put("supportUrl", value(profile.getSupportUrl()));
        snapshot.put("communityUrl", value(profile.getCommunityUrl()));
        snapshot.put("complianceNote", value(profile.getComplianceNote()));
        snapshot.put("operationsStatus", value(profile.getOperationsStatus()));
        return snapshot;
    }

    private Map<String, Object> buildMediaSnapshot(GameMediaAsset row) {
        if (row == null || row.getId() == null) {
            return Map.of();
        }
        return Map.of(
                "id", row.getId(),
                "assetGroup", value(row.getAssetGroup()),
                "assetRole", value(row.getAssetRole()),
                "mediaType", value(row.getMediaType()),
                "assetStatus", value(row.getAssetStatus()),
                "title", value(row.getTitle()),
                "url", value(row.getUrl()),
                "locale", value(row.getLocale()),
                "sortOrder", row.getSortOrder() == null ? 0 : row.getSortOrder(),
                "primary", Boolean.TRUE.equals(row.getPrimary())
        );
    }

    private List<String> normalizeHighlights(List<String> values) {
        if (values == null) {
            return List.of();
        }
        List<String> sanitized = values.stream()
                .map(value -> trim(value, 120))
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        return sanitized.size() > 8 ? null : sanitized;
    }

    private List<String> readHighlights(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String writeHighlights(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private boolean allUrlsValid(String... values) {
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!isValidUrl(value.trim())) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
        } catch (Exception exception) {
            return false;
        }
    }

    private String normalizeEnum(String value, Set<String> allowed) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return allowed.contains(normalized) ? normalized : null;
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

    private String value(String value) {
        return value == null ? "" : value;
    }

    private record ValidationResult(
            boolean success,
            String message,
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
    ) {
        private static ValidationResult error(String message) {
            return new ValidationResult(false, message, null, null, null, null, null, null, null, null, null, null, null, List.of(), null, null, null, null, null, null);
        }

        private static ValidationResult success(
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
        ) {
            return new ValidationResult(true, null, studioName, playerCountText, runtimeBannerUrl, runtimeLogoUrl, shareTitle, shareSubtitle, shareImageUrl, discoverCardCoverUrl, discoverCardLogoUrl, marketingTagline, marketingSummary, featureHighlights, targetAudience, supportEmail, supportUrl, communityUrl, complianceNote, operationsStatus);
        }
    }

    private record MediaValidationResult(
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
        private static MediaValidationResult error(String message) {
            return new MediaValidationResult(false, message, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        private static MediaValidationResult success(
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
            return new MediaValidationResult(true, null, assetGroup, assetRole, mediaType, assetStatus, title, description, url, actionTitle, actionUrl, width, height, sizeBytes, locale, sortOrder, primary);
        }
    }
}
