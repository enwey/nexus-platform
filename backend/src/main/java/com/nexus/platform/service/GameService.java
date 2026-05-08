package com.nexus.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.config.GamePackageProperties;
import com.nexus.platform.dto.GameUpdateCheckResponse;
import com.nexus.platform.dto.DeveloperMetricTrendPointDto;
import com.nexus.platform.dto.DeveloperOperationsDashboardDto;
import com.nexus.platform.dto.DeveloperRuntimeIssueDto;
import com.nexus.platform.dto.DeveloperVersionPreflightDto;
import com.nexus.platform.dto.GameMetadataUpdateRequest;
import com.nexus.platform.dto.DeveloperReleaseHealthDto;
import com.nexus.platform.dto.DeveloperUploadRetryRequest;
import com.nexus.platform.dto.DeveloperUploadTaskDto;
import com.nexus.platform.dto.OpsGameCategoryRequest;
import com.nexus.platform.dto.OpsGameCategoryResponse;
import com.nexus.platform.dto.OpsGameVisibilityUpdateRequest;
import com.nexus.platform.dto.PageResult;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.DeveloperGameMetricDaily;
import com.nexus.platform.entity.DeveloperRuntimeIssue;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.GameVersion;
import com.nexus.platform.entity.OpsGameCategory;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.DeveloperGameMetricDailyRepository;
import com.nexus.platform.repository.DeveloperRuntimeIssueRepository;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.GameVersionRepository;
import com.nexus.platform.repository.OpsGameCategoryRepository;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import java.net.URI;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class GameService {
    private static final String CATEGORY_STATUS_ENABLED = "ENABLED";
    private static final List<String> DEFAULT_GAME_CATEGORIES = List.of("動作射擊", "休閒益智", "角色扮演");
    private static final long MAX_UPLOAD_SIZE_BYTES = 100L * 1024 * 1024;
    private static final int MAX_SUBMIT_NOTE_LENGTH = 500;

    public record GameDownloadStream(GetObjectResponse stream, String filename) {
    }

    public record RuntimePackageKey(String key, String algorithm, String version, String format) {
    }

    public record RuntimePackageTicket(String ticket, long expiresInSeconds, String version, String format) {
    }

    private final GameRepository gameRepository;
    private final GameVersionRepository gameVersionRepository;
    private final OpsGameCategoryRepository gameCategoryRepository;
    private final DeveloperGameMetricDailyRepository developerGameMetricDailyRepository;
    private final DeveloperRuntimeIssueRepository developerRuntimeIssueRepository;
    private final MinioClient minioClient;
    private final AuditLogService auditLogService;
    private final UploadProcessingService uploadProcessingService;
    private final SecureGamePackageService secureGamePackageService;
    private final HostedMiniAppPackageService hostedMiniAppPackageService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final GamePackageProperties gamePackageProperties;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${platform.public-base-url}")
    private String publicBaseUrl;

    @Value("${platform.cdn-base-url:}")
    private String cdnBaseUrl;

    public Result<Game> uploadGame(MultipartFile file, String name, String description, User currentUser) {
        try {
            String validationError = validateUpload(file);
            if (validationError != null) {
                return Result.error(validationError);
            }

            ensureBucketExists();

            String appId = generateAppId();
            String storageKey = "games/runtime/" + appId + ".zip";
            String sourceStorageKey = "games/source/" + appId + ".zip";
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(sourceStorageKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType("application/zip")
                            .build()
            );

            Game game = new Game();
            game.setAppId(appId);
            game.setName(name);
            game.setDescription(description);
            game.setStorageKey(storageKey);
            game.setSourceStorageKey(sourceStorageKey);
            game.setUploadFileName(file.getOriginalFilename());
            game.setUploadFileSizeBytes(file.getSize());
            game.setUploadProcessingFailureReason(null);
            game.setUploadProcessingStartedAt(LocalDateTime.now());
            game.setUploadProcessingFinishedAt(null);
            game.setStatus(Game.GameStatus.PROCESSING);
            game.setDeveloperId(currentUser.getId());
            game.setDownloadUrl(buildControlPlaneDownloadUrl(appId));
            game = gameRepository.save(game);

            auditLogService.logGameAudit("GAME_UPLOAD_CREATE", currentUser, game, true, "Upload task created", "/game/upload");
            uploadProcessingService.processUpload(game.getId());
            return Result.success(game);
        } catch (Exception e) {
            return Result.error("Upload failed: " + e.getMessage());
        }
    }

    public Result<Game> uploadGameAsAdmin(
            MultipartFile file,
            String name,
            String description,
            String category,
            List<String> tags,
            Boolean requiresOnline,
            User currentUser,
            String requestUri
    ) {
        if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
            auditLogService.logOpsAudit("OPS_GAME_UPLOAD_CREATE", currentUser, null, null, false, "Permission denied", requestUri);
            return Result.error("No permission to create game upload");
        }
        try {
            String validationError = validateUpload(file);
            if (validationError != null) {
                auditLogService.logOpsAudit("OPS_GAME_UPLOAD_CREATE", currentUser, null, null, false, validationError, requestUri);
                return Result.error(validationError);
            }
            ensureBucketExists();

            String appId = generateAppId();
            String storageKey = "games/runtime/" + appId + ".zip";
            String sourceStorageKey = "games/source/" + appId + ".zip";
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(sourceStorageKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType("application/zip")
                            .build()
            );

            Game game = new Game();
            game.setAppId(appId);
            game.setName(defaultIfBlank(trimToNull(name, 100), "Pending package"));
            game.setDescription(trimToNull(description, 500));
            String normalizedCategory = normalizeCategory(category);
            if (category != null && !category.isBlank() && normalizedCategory == null) {
                auditLogService.logOpsAudit("OPS_GAME_UPLOAD_CREATE", currentUser, null, null, false, "Invalid category", requestUri);
                return Result.error("Invalid category");
            }
            game.setCategory("all".equalsIgnoreCase(defaultIfBlank(normalizedCategory, "")) ? "" : normalizedCategory);
            game.setTagsJson(writeTagsJson(tags));
            game.setRequiresOnline(Boolean.TRUE.equals(requiresOnline));
            game.setStorageKey(storageKey);
            game.setSourceStorageKey(sourceStorageKey);
            game.setUploadFileName(file.getOriginalFilename());
            game.setUploadFileSizeBytes(file.getSize());
            game.setUploadProcessingFailureReason(null);
            game.setUploadProcessingStartedAt(LocalDateTime.now());
            game.setUploadProcessingFinishedAt(null);
            game.setStatus(Game.GameStatus.PROCESSING);
            game.setDeveloperId(currentUser.getId());
            game.setDownloadUrl(buildControlPlaneDownloadUrl(appId));
            game = gameRepository.save(game);

            auditLogService.logOpsAudit("OPS_GAME_UPLOAD_CREATE", currentUser, game.getId(), game.getAppId(), true, "Created game upload task", requestUri);
            uploadProcessingService.processUpload(game.getId());
            return Result.success(game);
        } catch (Exception e) {
            auditLogService.logOpsAudit("OPS_GAME_UPLOAD_CREATE", currentUser, null, null, false, e.getMessage(), requestUri);
            return Result.error("Upload failed: " + e.getMessage());
        }
    }

    public Result<GameVersion> uploadGameVersionAsAdmin(
            Long gameId,
            MultipartFile file,
            String versionName,
            User currentUser,
            String requestUri
    ) {
        if (gameId == null || gameId <= 0) {
            return Result.error("Invalid game id");
        }
        if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
            auditLogService.logOpsAudit("OPS_GAME_VERSION_UPLOAD", currentUser, gameId, null, false, "Permission denied", requestUri);
            return Result.error("No permission to upload game version");
        }

        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            auditLogService.logOpsAudit("OPS_GAME_VERSION_UPLOAD", currentUser, gameId, null, false, "Game not found", requestUri);
            return Result.error("Game not found");
        }

        String validationError = validateUpload(file);
        if (validationError != null) {
            auditLogService.logOpsAudit("OPS_GAME_VERSION_UPLOAD", currentUser, game.getId(), game.getAppId(), false, validationError, requestUri);
            return Result.error(validationError);
        }
        if (gameVersionRepository.existsByGameIdAndStatus(game.getId(), GameVersion.VersionStatus.PROCESSING)) {
            auditLogService.logOpsAudit("OPS_GAME_VERSION_UPLOAD", currentUser, game.getId(), game.getAppId(), false, "Another version is still processing", requestUri);
            return Result.error("Another version is still processing");
        }

        String normalizedVersionName = normalizeVersionNameForUpload(versionName);
        if (versionName != null && normalizedVersionName == null) {
            auditLogService.logOpsAudit("OPS_GAME_VERSION_UPLOAD", currentUser, game.getId(), game.getAppId(), false, "Invalid version name", requestUri);
            return Result.error("Invalid version name");
        }
        if (normalizedVersionName == null) {
            normalizedVersionName = buildNextVersionName(game.getId());
        }
        final String resolvedVersionName = normalizedVersionName;

        List<GameVersion> existingVersions = gameVersionRepository.findByGameIdOrderByCreatedAtDesc(game.getId());
        GameVersion version = existingVersions.stream()
                .filter(item -> resolvedVersionName.equalsIgnoreCase(item.getVersionName()))
                .findFirst()
                .orElse(null);

        if (version != null && (version.getStatus() == GameVersion.VersionStatus.SUBMITTED || version.getStatus() == GameVersion.VersionStatus.APPROVED)) {
            auditLogService.logOpsAudit("OPS_GAME_VERSION_UPLOAD", currentUser, game.getId(), game.getAppId(), false, "Version name already exists in an active state", requestUri);
            return Result.error("Version name already exists in an active state");
        }

        try {
            ensureBucketExists();
            String objectSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String storageKey = "games/runtime/" + game.getAppId() + "/" + resolvedVersionName + "-" + objectSuffix + ".zip";
            String sourceStorageKey = "games/source/" + game.getAppId() + "/" + resolvedVersionName + "-" + objectSuffix + ".zip";
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(sourceStorageKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType("application/zip")
                            .build()
            );

            boolean created = false;
            if (version == null) {
                version = new GameVersion();
                version.setGameId(game.getId());
                version.setVersionName(resolvedVersionName);
                version.setForcedUpdate(Boolean.FALSE);
                created = true;
            }

            version.setEntryFile(defaultIfBlank(version.getEntryFile(), "index.html"));
            version.setStorageKey(storageKey);
            version.setSourceStorageKey(sourceStorageKey);
            version.setDownloadUrl(buildControlPlaneDownloadUrl(game.getAppId()));
            version.setSubmitNote(null);
            version.setAuditReason(null);
            version.setAssignedReviewerId(null);
            version.setAssignedAt(null);
            version.setStatus(GameVersion.VersionStatus.PROCESSING);
            version = gameVersionRepository.save(version);

            uploadProcessingService.processVersionUpload(version.getId());
            auditLogService.logOpsAudit(
                    created ? "OPS_GAME_VERSION_UPLOAD_CREATE" : "OPS_GAME_VERSION_UPLOAD_REPLACE",
                    currentUser,
                    game.getId(),
                    game.getAppId(),
                    true,
                    "Queued version " + resolvedVersionName + " for processing",
                    requestUri
            );
            return Result.success(version);
        } catch (Exception e) {
            auditLogService.logOpsAudit("OPS_GAME_VERSION_UPLOAD", currentUser, game.getId(), game.getAppId(), false, e.getMessage(), requestUri);
            return Result.error("Version upload failed: " + e.getMessage());
        }
    }

    public Result<GameUpdateCheckResponse> checkUpdate(String appId, String localVersion) {
        Game game = gameRepository.findByAppId(appId);
        if (game == null || !isFrontendVisibleForPublic(game)) {
            return Result.error("Game not found");
        }

        GameVersion latestApproved = gameVersionRepository
                .findTopByGameIdAndStatusOrderByCreatedAtDesc(game.getId(), GameVersion.VersionStatus.APPROVED)
                .orElse(null);

        String latestVersion = latestApproved == null ? game.getVersion() : latestApproved.getVersionName();
        String latestMd5 = latestApproved == null ? game.getMd5() : latestApproved.getMd5();
        boolean forceUpdate = latestApproved != null && Boolean.TRUE.equals(latestApproved.getForcedUpdate());

        boolean hasUpdate = compareVersion(
                latestVersion == null ? "0.0.0" : latestVersion,
                localVersion == null ? "0.0.0" : localVersion
        ) > 0;

        GameUpdateCheckResponse response = new GameUpdateCheckResponse();
        response.setHasUpdate(hasUpdate);
        response.setForceUpdate(hasUpdate && forceUpdate);
        response.setUpdateReady(hasUpdate);
        response.setLatestVersion(latestVersion);
        response.setMd5(latestMd5);
        response.setDownloadUrl(hasUpdate ? buildControlPlaneDownloadUrl(game.getAppId()) : null);
        return Result.success(response);
    }

    public Result<List<Game>> getGameList(User currentUser) {
        if (currentUser != null && currentUser.getRole() == User.UserRole.ADMIN) {
            List<Game> games = gameRepository.findAllByOrderByCreatedAtDesc();
            games.forEach(this::normalizeClientUrls);
            return Result.success(games);
        }
        List<Game> games = gameRepository.findByStatusOrderByCreatedAtDesc(Game.GameStatus.APPROVED).stream()
                .filter(this::isFrontendVisibleForPublic)
                .toList();
        games.forEach(this::normalizeClientUrls);
        return Result.success(games);
    }

    public Result<PageResult<Game>> getGameListPaged(User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<Game> gamesPage;
        if (currentUser != null && currentUser.getRole() == User.UserRole.ADMIN) {
            gamesPage = gameRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            gamesPage = gameRepository.findByStatus(Game.GameStatus.APPROVED, pageable);
        }
        List<Game> pageContent = gamesPage.getContent();
        if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
            pageContent = pageContent.stream().filter(this::isFrontendVisibleForPublic).toList();
        }
        pageContent.forEach(this::normalizeClientUrls);
        return Result.success(new PageResult<>(
                pageContent,
                gamesPage.getNumber(),
                gamesPage.getSize(),
                gamesPage.getTotalElements(),
                gamesPage.getTotalPages()
        ));
    }

    public Result<Game> getGameByAppId(String appId) {
        Game game = gameRepository.findByAppId(appId);
        if (game == null || !isFrontendVisibleForPublic(game)) {
            return Result.error("Game not found");
        }
        normalizeClientUrls(game);
        attachHostedLocales(game);
        return Result.success(game);
    }

    public Result<List<String>> getGameCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("all");
        categories.addAll(listEnabledGameCategoryNames());
        return Result.success(categories);
    }

    public Result<List<OpsGameCategoryResponse>> listGameCategoryOptions() {
        ensureDefaultGameCategorySeed();
        List<OpsGameCategoryResponse> rows = gameCategoryRepository
                .findByStatusOrderBySortOrderAscUpdatedAtDesc(CATEGORY_STATUS_ENABLED)
                .stream()
                .map(this::toGameCategoryResponse)
                .toList();
        return Result.success(rows);
    }

    public Result<List<OpsGameCategoryResponse>> createGameCategory(OpsGameCategoryRequest request) {
        String normalizedName = normalizeCategoryName(request == null ? null : request.name());
        if (normalizedName == null) {
            return Result.error("Category name is required");
        }
        if (gameCategoryRepository.existsByNameIgnoreCase(normalizedName)) {
            return Result.error("Category name already exists");
        }
        OpsGameCategory row = new OpsGameCategory();
        row.setName(normalizedName);
        row.setSortOrder(categorySortOrder(request == null ? null : request.sortOrder()));
        row.setStatus(CATEGORY_STATUS_ENABLED);
        gameCategoryRepository.save(row);
        return listGameCategoryOptions();
    }

    public Result<List<OpsGameCategoryResponse>> updateGameCategory(Long id, OpsGameCategoryRequest request) {
        if (id == null) {
            return Result.error("Category id is required");
        }
        OpsGameCategory row = gameCategoryRepository.findById(id).orElse(null);
        if (row == null || !CATEGORY_STATUS_ENABLED.equals(row.getStatus())) {
            return Result.error("Category not found");
        }
        String normalizedName = normalizeCategoryName(request == null ? null : request.name());
        if (normalizedName == null) {
            return Result.error("Category name is required");
        }
        if (gameCategoryRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, row.getId())) {
            return Result.error("Category name already exists");
        }
        row.setName(normalizedName);
        row.setSortOrder(categorySortOrder(request == null ? null : request.sortOrder()));
        gameCategoryRepository.save(row);
        return listGameCategoryOptions();
    }

    public Result<List<OpsGameCategoryResponse>> deleteGameCategory(Long id) {
        if (id == null) {
            return Result.error("Category id is required");
        }
        OpsGameCategory row = gameCategoryRepository.findById(id).orElse(null);
        if (row == null || !CATEGORY_STATUS_ENABLED.equals(row.getStatus())) {
            return Result.error("Category not found");
        }
        long usedCount = gameRepository.countByCategoryIgnoreCase(row.getName());
        if (usedCount > 0) {
            return Result.error("Category is used by games and cannot be deleted");
        }
        gameCategoryRepository.delete(row);
        return listGameCategoryOptions();
    }

    public Result<Game> updateGameMetadata(Long gameId, GameMetadataUpdateRequest request, User currentUser) {
        return updateGameMetadataInternal(gameId, request, currentUser, "/game/" + gameId + "/metadata", false);
    }

    public Result<Game> updateGameVisibility(
            Long gameId,
            OpsGameVisibilityUpdateRequest request,
            User currentUser,
            String requestUri
    ) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            auditLogService.logGameAudit("GAME_VISIBILITY_UPDATE", currentUser, null, false, "Game not found", requestUri);
            return Result.error("Game not found");
        }
        if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
            auditLogService.logGameAudit("GAME_VISIBILITY_UPDATE", currentUser, game, false, "Permission denied", requestUri);
            return Result.error("No permission to update game visibility");
        }
        String status = normalizeVisibilityStatus(request == null ? null : request.visibilityStatus());
        if (status == null) {
            auditLogService.logGameAudit("GAME_VISIBILITY_UPDATE", currentUser, game, false, "Invalid visibility status", requestUri);
            return Result.error("Invalid visibility status");
        }
        String reason = request == null || request.reason() == null ? null : request.reason().trim();
        LocalDateTime until = request == null ? null : request.visibilityUntil();
        if (!"VISIBLE".equals(status)) {
            if (reason == null || reason.length() < 2) {
                auditLogService.logGameAudit("GAME_VISIBILITY_UPDATE", currentUser, game, false, "Reason must be at least 2 chars", requestUri);
                return Result.error("Reason must be at least 2 chars");
            }
            if (reason.length() > 256) {
                auditLogService.logGameAudit("GAME_VISIBILITY_UPDATE", currentUser, game, false, "Reason is too long", requestUri);
                return Result.error("Reason is too long");
            }
        }
        if ("BLOCKED".equals(status) && until != null && until.isBefore(LocalDateTime.now())) {
            auditLogService.logGameAudit("GAME_VISIBILITY_UPDATE", currentUser, game, false, "Visibility until cannot be in the past", requestUri);
            return Result.error("Visibility until cannot be in the past");
        }

        game.setVisibilityStatus(status);
        game.setVisibilityReason("VISIBLE".equals(status) ? null : reason);
        game.setVisibilityUntil("BLOCKED".equals(status) ? until : null);
        gameRepository.save(game);

        recordVisibilityAudit(game, currentUser, "Set visibility to " + status + (reason == null ? "" : " (" + reason + ")"), requestUri);
        return Result.success(game);
    }

    public void recordVisibilityAudit(Game game, User currentUser, String reason, String requestUri) {
        auditLogService.logGameAudit(
                "GAME_VISIBILITY_UPDATE",
                currentUser,
                game,
                true,
                reason,
                requestUri
        );
    }

    public void recordReviewAudit(String action, User currentUser, Game game, GameVersion version, String reason, String requestUri) {
        String finalReason = reason;
        if (version != null && version.getAssignedReviewerId() != null) {
            finalReason = (finalReason == null || finalReason.isBlank() ? "" : finalReason + " | ")
                    + "reviewer=" + version.getAssignedReviewerId();
        }
        auditLogService.logGameAudit(action, currentUser, game, true, finalReason, requestUri);
    }

    public Result<Game> updateGameMetadataInternal(
            Long gameId,
            GameMetadataUpdateRequest request,
            User currentUser,
            String requestUri,
            boolean adminMode
    ) {
        if (request == null) {
            return Result.error("Request body is required");
        }
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            auditLogService.logGameAudit("GAME_METADATA_UPDATE", currentUser, null, false, "Game not found", requestUri);
            return Result.error("Game not found");
        }
        boolean canEdit = currentUser != null && (currentUser.getRole() == User.UserRole.ADMIN
                || currentUser.getId().equals(game.getDeveloperId()));
        if (!canEdit) {
            auditLogService.logGameAudit("GAME_METADATA_UPDATE", currentUser, game, false, "Permission denied", requestUri);
            return Result.error("No permission to edit this game");
        }
        if (request.name() != null && request.name().trim().length() > 100) {
            auditLogService.logGameAudit("GAME_METADATA_UPDATE", currentUser, game, false, "Name too long", requestUri);
            return Result.error("Game name length must be <= 100");
        }
        if (request.description() != null && request.description().trim().length() > 500) {
            auditLogService.logGameAudit("GAME_METADATA_UPDATE", currentUser, game, false, "Description too long", requestUri);
            return Result.error("Game description length must be <= 500");
        }
        if (request.iconUrl() != null) {
            String iconUrl = request.iconUrl().trim();
            if (iconUrl.length() > 512) {
                auditLogService.logGameAudit("GAME_METADATA_UPDATE", currentUser, game, false, "Icon URL too long", requestUri);
                return Result.error("Icon URL length must be <= 512");
            }
            if (!iconUrl.isBlank() && !(iconUrl.startsWith("http://") || iconUrl.startsWith("https://") || iconUrl.startsWith("/"))) {
                auditLogService.logGameAudit("GAME_METADATA_UPDATE", currentUser, game, false, "Invalid icon URL", requestUri);
                return Result.error("Icon URL must start with http://, https://, or /");
            }
        }

        if (request.name() != null && !request.name().isBlank()) {
            game.setName(request.name().trim());
        }
        if (request.description() != null) {
            game.setDescription(request.description().trim());
        }
        if (request.iconUrl() != null) {
            game.setIconUrl(request.iconUrl().trim());
        }
        if (request.version() != null && !request.version().isBlank()) {
            game.setVersion(request.version().trim());
        }
        if (request.category() != null) {
            String normalizedCategory = normalizeCategory(request.category());
            if (normalizedCategory == null) {
                return Result.error("Invalid category");
            }
            game.setCategory(normalizedCategory);
        }
        if (request.tags() != null) {
            if (request.tags().size() > 20) {
                auditLogService.logGameAudit("GAME_METADATA_UPDATE", currentUser, game, false, "Too many tags", requestUri);
                return Result.error("Tags count must be <= 20");
            }
            String serialized = request.tags().stream()
                    .map(tag -> tag == null ? "" : tag.trim())
                    .filter(tag -> !tag.isBlank())
                    .filter(tag -> tag.length() <= 32)
                    .distinct()
                    .limit(20)
                    .collect(Collectors.joining(","));
            game.setTagsJson(serialized);
        }
        if (request.requiresOnline() != null) {
            game.setRequiresOnline(Boolean.TRUE.equals(request.requiresOnline()));
        }

        game = gameRepository.save(game);
        normalizeClientUrls(game);
        String auditReason = adminMode ? "Metadata updated from ops portal" : "Metadata updated";
        auditLogService.logGameAudit("GAME_METADATA_UPDATE", currentUser, game, true, auditReason, requestUri);
        return Result.success(game);
    }

    public Result<List<Game>> getDeveloperGames(Long developerId, User currentUser) {
        if (currentUser == null) {
            return Result.error("Missing valid login token");
        }
        if (currentUser.getRole() != User.UserRole.ADMIN && !currentUser.getId().equals(developerId)) {
            return Result.error("No permission to view other developer games");
        }
        List<Game> games = gameRepository.findByDeveloperIdOrderByCreatedAtDesc(developerId);
        games.forEach(this::normalizeClientUrls);
        return Result.success(games);
    }

    public Result<PageResult<Game>> getDeveloperGamesPaged(Long developerId, User currentUser, int page, int size) {
        if (currentUser == null) {
            return Result.error("Missing valid login token");
        }
        if (currentUser.getRole() != User.UserRole.ADMIN && !currentUser.getId().equals(developerId)) {
            return Result.error("No permission to view other developer games");
        }
        Page<Game> gamesPage = gameRepository.findByDeveloperIdOrderByCreatedAtDesc(
                developerId, PageRequest.of(Math.max(page, 0), clampSize(size))
        );
        gamesPage.getContent().forEach(this::normalizeClientUrls);
        return Result.success(new PageResult<>(
                gamesPage.getContent(),
                gamesPage.getNumber(),
                gamesPage.getSize(),
                gamesPage.getTotalElements(),
                gamesPage.getTotalPages()
        ));
    }

    public Result<DeveloperReleaseHealthDto> getDeveloperReleaseHealth(Long developerId, User currentUser) {
        if (currentUser == null) {
            return Result.error("Missing valid login token");
        }
        if (currentUser.getRole() != User.UserRole.ADMIN && !currentUser.getId().equals(developerId)) {
            return Result.error("No permission to view other developer release health");
        }

        List<Game> games = gameRepository.findByDeveloperIdOrderByCreatedAtDesc(developerId);
        List<DeveloperReleaseHealthDto.GameReleaseItem> rows = games.stream()
                .map(this::toDeveloperReleaseItem)
                .toList();

        int totalGames = rows.size();
        int liveGames = (int) rows.stream().filter(row -> "LIVE".equals(row.frontendState())).count();
        int processingGames = (int) rows.stream().filter(row -> "PROCESSING".equals(row.gameStatus())).count();
        int pendingReviewGames = (int) rows.stream().filter(row -> "SUBMITTED".equals(row.latestVersionStatus())).count();
        int blockedGames = (int) rows.stream().filter(row -> row.blockingReason() != null && !row.blockingReason().isBlank()).count();
        int releasableGames = (int) rows.stream().filter(row -> Boolean.TRUE.equals(row.canSubmit())).count();

        return Result.success(new DeveloperReleaseHealthDto(
                new DeveloperReleaseHealthDto.Overview(
                        totalGames,
                        liveGames,
                        processingGames,
                        pendingReviewGames,
                        blockedGames,
                        releasableGames
                ),
                rows
        ));
    }

    public Result<List<DeveloperUploadTaskDto>> getDeveloperUploadTasks(Long developerId, User currentUser) {
        Result<List<Game>> gamesResult = getDeveloperGames(developerId, currentUser);
        if (gamesResult.getCode() != 0) {
            return Result.error(gamesResult.getMessage());
        }
        List<DeveloperUploadTaskDto> rows = gamesResult.getData().stream()
                .map(this::toDeveloperUploadTask)
                .toList();
        return Result.success(rows);
    }

    public Result<DeveloperUploadTaskDto> retryDeveloperUploadTask(
            Long developerId,
            Long gameId,
            User currentUser,
            DeveloperUploadRetryRequest request,
            String requestUri
    ) {
        if (currentUser == null) {
            return Result.error("Missing valid login token");
        }
        if (currentUser.getRole() != User.UserRole.ADMIN && !currentUser.getId().equals(developerId)) {
            return Result.error("No permission to retry other developer tasks");
        }
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null || !developerId.equals(game.getDeveloperId())) {
            auditLogService.logOpsAudit("DEV_UPLOAD_RETRY", currentUser, null, "game:" + gameId, false, "Upload task not found", requestUri);
            return Result.error("Upload task not found");
        }
        String confirmText = request == null || request.confirmText() == null ? null : request.confirmText().trim();
        if (!"RETRY".equalsIgnoreCase(confirmText)) {
            auditLogService.logOpsAudit("DEV_UPLOAD_RETRY", currentUser, game.getId(), game.getAppId(), false, "Confirmation text mismatch", requestUri);
            return Result.error("Confirmation text mismatch");
        }
        if (game.getStatus() == Game.GameStatus.PROCESSING) {
            auditLogService.logOpsAudit("DEV_UPLOAD_RETRY", currentUser, game.getId(), game.getAppId(), false, "Upload task is already processing", requestUri);
            return Result.error("Upload task is already processing");
        }
        if (game.getUploadProcessingFailureReason() == null || game.getUploadProcessingFailureReason().isBlank()) {
            auditLogService.logOpsAudit("DEV_UPLOAD_RETRY", currentUser, game.getId(), game.getAppId(), false, "Only failed upload tasks can be retried", requestUri);
            return Result.error("Only failed upload tasks can be retried");
        }
        if (game.getSourceStorageKey() == null || game.getSourceStorageKey().isBlank()) {
            auditLogService.logOpsAudit("DEV_UPLOAD_RETRY", currentUser, game.getId(), game.getAppId(), false, "Source package is missing", requestUri);
            return Result.error("Source package is missing");
        }
        int retryCount = game.getUploadProcessingRetryCount() == null ? 0 : game.getUploadProcessingRetryCount();
        if (retryCount >= 3) {
            auditLogService.logOpsAudit("DEV_UPLOAD_RETRY", currentUser, game.getId(), game.getAppId(), false, "Retry limit reached", requestUri);
            return Result.error("Retry limit reached");
        }

        game.setStatus(Game.GameStatus.PROCESSING);
        game.setUploadProcessingRetryCount(retryCount + 1);
        game.setUploadProcessingFailureReason(null);
        game.setUploadProcessingStartedAt(LocalDateTime.now());
        game.setUploadProcessingFinishedAt(null);
        gameRepository.save(game);
        auditLogService.logOpsAudit("DEV_UPLOAD_RETRY", currentUser, game.getId(), game.getAppId(), true, "Retry upload processing", requestUri);
        uploadProcessingService.processUpload(game.getId());
        return Result.success(toDeveloperUploadTask(game));
    }

    public Result<DeveloperOperationsDashboardDto> getDeveloperOperationsDashboard(Long developerId, User currentUser) {
        if (currentUser == null) {
            return Result.error("Missing valid login token");
        }
        if (currentUser.getRole() != User.UserRole.ADMIN && !currentUser.getId().equals(developerId)) {
            return Result.error("No permission to view other developer analytics");
        }

        List<Game> games = gameRepository.findByDeveloperIdOrderByCreatedAtDesc(developerId);
        Map<Long, Game> gameMap = games.stream().collect(Collectors.toMap(Game::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        LocalDate fromDate = LocalDate.now().minusDays(6);
        List<DeveloperGameMetricDaily> metrics = developerGameMetricDailyRepository
                .findByDeveloperIdAndMetricDateGreaterThanEqualOrderByMetricDateAsc(developerId, fromDate);
        List<DeveloperRuntimeIssue> issues = developerRuntimeIssueRepository.findByDeveloperIdOrderByLastOccurredAtDesc(developerId);

        List<DeveloperMetricTrendPointDto> trend = buildMetricTrend(metrics, fromDate);
        List<DeveloperRuntimeIssueDto> runtimeIssues = issues.stream()
                .map(issue -> toRuntimeIssueDto(issue, gameMap.get(issue.getGameId())))
                .toList();
        List<DeveloperOperationsDashboardDto.GameOpsRow> gameRows = buildGameOpsRows(metrics, runtimeIssues, gameMap);

        int installs7d = metrics.stream().mapToInt(item -> safeInt(item.getInstalls())).sum();
        int launches7d = metrics.stream().mapToInt(item -> safeInt(item.getLaunches())).sum();
        int activeUsers7d = metrics.stream().mapToInt(item -> safeInt(item.getActiveUsers())).sum();
        int installFailures7d = metrics.stream().mapToInt(item -> safeInt(item.getInstallFailures())).sum();
        int launchFailures7d = metrics.stream().mapToInt(item -> safeInt(item.getLaunchFailures())).sum();
        BigDecimal avgSessionMinutes7d = metrics.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(metrics.stream().map(item -> item.getAvgSessionMinutes() == null ? BigDecimal.ZERO : item.getAvgSessionMinutes())
                        .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue() / metrics.size()).setScale(2, java.math.RoundingMode.HALF_UP);
        int openIssueCount = (int) issues.stream().filter(item -> !"RESOLVED".equalsIgnoreCase(item.getIssueStatus())).count();
        int impactedGames = (int) runtimeIssues.stream().filter(item -> !"RESOLVED".equalsIgnoreCase(item.issueStatus())).map(DeveloperRuntimeIssueDto::gameId).distinct().count();

        return Result.success(new DeveloperOperationsDashboardDto(
                new DeveloperOperationsDashboardDto.Overview(
                        installs7d,
                        launches7d,
                        activeUsers7d,
                        installFailures7d,
                        launchFailures7d,
                        avgSessionMinutes7d,
                        impactedGames,
                        openIssueCount
                ),
                trend,
                gameRows,
                runtimeIssues
        ));
    }

    public Result<DeveloperVersionPreflightDto> getDeveloperVersionPreflight(
            Long developerId,
            Long gameId,
            Long versionId,
            User currentUser,
            Boolean forceUpdate
    ) {
        if (currentUser == null) {
            return Result.error("Missing valid login token");
        }
        if (currentUser.getRole() != User.UserRole.ADMIN && !currentUser.getId().equals(developerId)) {
            return Result.error("No permission to view other developer preflight data");
        }
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null || !developerId.equals(game.getDeveloperId())) {
            return Result.error("Game not found");
        }
        GameVersion version = gameVersionRepository.findByIdAndGameId(versionId, gameId).orElse(null);
        if (version == null) {
            return Result.error("Game version not found");
        }
        return Result.success(buildDeveloperVersionPreflight(game, version, forceUpdate));
    }

    public Result<String> getPresignedDownloadUrl(String appId, User currentUser) {
        try {
            Game game = gameRepository.findByAppId(appId);
            if (game == null) {
                return Result.error("Game not found");
            }

            boolean canDownload = game.getStatus() == Game.GameStatus.APPROVED
                    || (currentUser != null && (currentUser.getRole() == User.UserRole.ADMIN
                    || currentUser.getId().equals(game.getDeveloperId())));
            if (currentUser == null && !isFrontendVisibleForPublic(game)) {
                return Result.error("Current game cannot be downloaded");
            }
            if (!canDownload) {
                return Result.error("Current game cannot be downloaded");
            }

            if (cdnBaseUrl != null && !cdnBaseUrl.isBlank()) {
                String base = cdnBaseUrl.replaceAll("/+$", "");
                return Result.success(base + "/" + game.getStorageKey());
            }

            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(game.getStorageKey())
                            .expiry((int) Duration.ofMinutes(15).toSeconds())
                            .build()
            );
            return Result.success(normalizePresignedUrlForClient(url));
        } catch (Exception e) {
            return Result.error("Failed to generate download url: " + e.getMessage());
        }
    }

    public Result<GameDownloadStream> getDownloadStream(String appId, User currentUser) {
        try {
            Game game = gameRepository.findByAppId(appId);
            if (game == null) {
                return Result.error("Game not found");
            }

            boolean canDownload = game.getStatus() == Game.GameStatus.APPROVED
                    || (currentUser != null && (currentUser.getRole() == User.UserRole.ADMIN
                    || currentUser.getId().equals(game.getDeveloperId())));
            if (currentUser == null && !isFrontendVisibleForPublic(game)) {
                return Result.error("Current game cannot be downloaded");
            }
            if (!canDownload) {
                return Result.error("Current game cannot be downloaded");
            }

            GetObjectResponse stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(game.getStorageKey())
                            .build()
            );
            String filename = game.getAppId() + ".zip";
            return Result.success(new GameDownloadStream(stream, filename));
        } catch (Exception e) {
            return Result.error("Failed to open download stream: " + e.getMessage());
        }
    }

    public Result<RuntimePackageTicket> issueRuntimePackageTicket(
            String appId,
            String version,
            User currentUser,
            String deviceId,
            String supportedFormatsHeader
    ) {
        if (currentUser == null) {
            return Result.error("Missing valid login token");
        }
        if (deviceId == null || deviceId.isBlank()) {
            return Result.error("Device id is required");
        }

        ResolvedPackageContext context = resolvePackageContext(appId, version);
        if (context.error() != null) {
            return Result.error(context.error());
        }
        if (!canBypassFrontendVisibility(currentUser, context.game()) && !isFrontendVisibleForPublic(context.game())) {
            return Result.error("Current game is blocked by platform control");
        }
        Set<String> supportedFormats = parseSupportedFormats(supportedFormatsHeader);
        if (supportedFormats.isEmpty() == false && supportedFormats.contains(context.packageFormat()) == false) {
            return Result.error("Current client does not support package format: " + context.packageFormat());
        }

        try {
            long ttlSeconds = Math.max(30L, gamePackageProperties.getRuntimeTicketTtlSeconds());
            long expiresAtEpoch = (System.currentTimeMillis() / 1000L) + ttlSeconds;
            String jti = UUID.randomUUID().toString();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("jti", jti);
            payload.put("uid", currentUser.getId());
            payload.put("did", deviceId.trim());
            payload.put("appId", context.game().getAppId());
            payload.put("version", context.version());
            payload.put("format", context.packageFormat());
            payload.put("exp", expiresAtEpoch);
            String ticket = signRuntimeTicket(payload);
            stringRedisTemplate.opsForValue().set(
                    runtimeTicketKey(jti),
                    "1",
                    Duration.ofSeconds(ttlSeconds)
            );
            return Result.success(new RuntimePackageTicket(
                    ticket,
                    ttlSeconds,
                    context.version(),
                    context.packageFormat()
            ));
        } catch (Exception e) {
            return Result.error("Failed to issue runtime ticket: " + e.getMessage());
        }
    }

    public Result<RuntimePackageKey> redeemRuntimePackageKey(
            String appId,
            String version,
            String ticket,
            User currentUser,
            String deviceId,
            String supportedFormatsHeader
    ) {
        if (currentUser == null) {
            return Result.error("Missing valid login token");
        }
        if (ticket == null || ticket.isBlank()) {
            return Result.error("Runtime ticket is required");
        }
        if (deviceId == null || deviceId.isBlank()) {
            return Result.error("Device id is required");
        }

        try {
            Map<String, Object> payload = parseAndVerifyRuntimeTicket(ticket.trim());
            long ticketUserId = parseLongValue(payload.get("uid"));
            String ticketDeviceId = stringValue(payload.get("did"));
            String ticketAppId = stringValue(payload.get("appId"));
            String ticketVersion = stringValue(payload.get("version"));
            String jti = stringValue(payload.get("jti"));
            long expiresAtEpoch = parseLongValue(payload.get("exp"));
            if (jti.isBlank()) {
                return Result.error("Runtime ticket is invalid");
            }
            if (expiresAtEpoch <= (System.currentTimeMillis() / 1000L)) {
                return Result.error("Runtime ticket is expired");
            }
            String existing = stringRedisTemplate.opsForValue().get(runtimeTicketKey(jti));
            if (existing == null || existing.isBlank()) {
                return Result.error("Runtime ticket is expired");
            }
            if (ticketUserId != currentUser.getId()
                    || !deviceId.trim().equals(ticketDeviceId)
                    || !appId.equals(ticketAppId)) {
                return Result.error("Runtime ticket is invalid");
            }
            if (version != null && !version.isBlank() && !version.trim().equals(ticketVersion)) {
                return Result.error("Runtime ticket does not match requested version");
            }

            ResolvedPackageContext context = resolvePackageContext(appId, ticketVersion);
            if (context.error() != null) {
                return Result.error(context.error());
            }
            if (!canBypassFrontendVisibility(currentUser, context.game()) && !isFrontendVisibleForPublic(context.game())) {
                return Result.error("Current game is blocked by platform control");
            }
            Set<String> supportedFormats = parseSupportedFormats(supportedFormatsHeader);
            if (supportedFormats.isEmpty() == false && supportedFormats.contains(context.packageFormat()) == false) {
                return Result.error("Current client does not support package format: " + context.packageFormat());
            }

            stringRedisTemplate.delete(runtimeTicketKey(jti));
            String key = secureGamePackageService.unwrapContentKey(context.wrappedKey(), context.wrappedNonce());
            return Result.success(new RuntimePackageKey(
                    key,
                    "AES-256-GCM",
                    context.version(),
                    context.packageFormat()
            ));
        } catch (Exception e) {
            return Result.error("Failed to redeem runtime ticket: " + e.getMessage());
        }
    }

    public Result<List<GameVersion>> getGameVersions(Long gameId, User currentUser) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            return Result.error("Game not found");
        }
        if (currentUser == null) {
            return Result.error("Missing valid login token");
        }
        boolean canView = currentUser.getRole() == User.UserRole.ADMIN
                || currentUser.getId().equals(game.getDeveloperId());
        if (!canView) {
            return Result.error("No permission to view game versions");
        }
        List<GameVersion> versions = gameVersionRepository.findByGameIdOrderByCreatedAtDesc(gameId);
        versions.forEach(this::attachHostedManifestReport);
        return Result.success(versions);
    }

    public Result<Void> submitGameForAudit(
            Long id,
            User currentUser,
            String requestUri,
            String note,
            Boolean forceUpdate
    ) {
        Game game = gameRepository.findById(id).orElse(null);
        if (game == null) {
            auditLogService.logGameAudit("GAME_SUBMIT", currentUser, null, false, "Game not found", requestUri);
            return Result.error("Game not found");
        }

        boolean canSubmit = currentUser.getRole() == User.UserRole.ADMIN
                || currentUser.getId().equals(game.getDeveloperId());
        if (!canSubmit) {
            auditLogService.logGameAudit("GAME_SUBMIT", currentUser, game, false, "Permission denied", requestUri);
            return Result.error("No permission to submit this game");
        }
        String noteValidationError = validateSubmitNote(note);
        if (noteValidationError != null) {
            auditLogService.logGameAudit("GAME_SUBMIT", currentUser, game, false, noteValidationError, requestUri);
            return Result.error(noteValidationError);
        }
        if (game.getStatus() == Game.GameStatus.PROCESSING) {
            auditLogService.logGameAudit("GAME_SUBMIT", currentUser, game, false, "Game package is still processing", requestUri);
            return Result.error("Game package is still processing");
        }
        if (gameVersionRepository.existsByGameIdAndStatus(game.getId(), GameVersion.VersionStatus.SUBMITTED)) {
            auditLogService.logGameAudit("GAME_SUBMIT", currentUser, game, false, "Another version is already under review", requestUri);
            return Result.error("Another version is already under review");
        }

        GameVersion latest = gameVersionRepository.findTopByGameIdOrderByCreatedAtDesc(game.getId()).orElse(null);
        if (latest == null) {
            auditLogService.logGameAudit("GAME_SUBMIT", currentUser, game, false, "No version available", requestUri);
            return Result.error("No version available for submit");
        }
        if (!(latest.getStatus() == GameVersion.VersionStatus.DRAFT
                || latest.getStatus() == GameVersion.VersionStatus.REJECTED)) {
            auditLogService.logGameAudit("GAME_SUBMIT", currentUser, game, false, "Invalid status transition", requestUri);
            return Result.error("Current version status does not allow submit");
        }
        DeveloperVersionPreflightDto preflight = buildDeveloperVersionPreflight(game, latest, forceUpdate);
        if (!Boolean.TRUE.equals(preflight.ready())) {
            auditLogService.logGameAudit(
                    "GAME_SUBMIT",
                    currentUser,
                    game,
                    false,
                    preflight.blockingReason() == null ? "Version failed preflight checks" : preflight.blockingReason(),
                    requestUri
            );
            return Result.error(preflight.blockingReason() == null ? "Version failed preflight checks" : preflight.blockingReason());
        }

        latest.setStatus(GameVersion.VersionStatus.SUBMITTED);
        latest.setSubmitNote(note == null ? null : note.trim());
        latest.setForcedUpdate(Boolean.TRUE.equals(forceUpdate));
        gameVersionRepository.save(latest);

        game.setStatus(Game.GameStatus.PENDING);
        game.setVersion(latest.getVersionName());
        game.setMd5(latest.getMd5());
        game.setSourceMd5(latest.getSourceMd5());
        game.setStorageKey(latest.getStorageKey());
        game.setSourceStorageKey(latest.getSourceStorageKey());
        game.setDownloadUrl(buildControlPlaneDownloadUrl(game.getAppId()));
        gameRepository.save(game);

        auditLogService.logGameAudit("GAME_SUBMIT", currentUser, game, true, "Submit for audit", requestUri);
        return Result.success();
    }

    public Result<Void> submitGameVersionForAudit(
            Long gameId,
            Long versionId,
            User currentUser,
            String requestUri,
            String note,
            Boolean forceUpdate
    ) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, null, false, "Game not found", requestUri);
            return Result.error("Game not found");
        }

        boolean canSubmit = currentUser.getRole() == User.UserRole.ADMIN
                || currentUser.getId().equals(game.getDeveloperId());
        if (!canSubmit) {
            auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, game, false, "Permission denied", requestUri);
            return Result.error("No permission to submit this game version");
        }
        String noteValidationError = validateSubmitNote(note);
        if (noteValidationError != null) {
            auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, game, false, noteValidationError, requestUri);
            return Result.error(noteValidationError);
        }
        if (game.getStatus() == Game.GameStatus.PROCESSING) {
            auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, game, false, "Game package is still processing", requestUri);
            return Result.error("Game package is still processing");
        }

        GameVersion version = gameVersionRepository.findByIdAndGameId(versionId, gameId).orElse(null);
        if (version == null) {
            auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, game, false, "Version not found", requestUri);
            return Result.error("Game version not found");
        }
        if (!(version.getStatus() == GameVersion.VersionStatus.DRAFT
                || version.getStatus() == GameVersion.VersionStatus.REJECTED)) {
            auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, game, false, "Invalid status transition", requestUri);
            return Result.error("Current version status does not allow submit");
        }
        if (gameVersionRepository.existsByGameIdAndStatus(game.getId(), GameVersion.VersionStatus.SUBMITTED)) {
            auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, game, false, "Another version is already under review", requestUri);
            return Result.error("Another version is already under review");
        }
        DeveloperVersionPreflightDto preflight = buildDeveloperVersionPreflight(game, version, forceUpdate);
        if (!Boolean.TRUE.equals(preflight.ready())) {
            auditLogService.logGameAudit(
                    "GAME_SUBMIT_VERSION",
                    currentUser,
                    game,
                    false,
                    preflight.blockingReason() == null ? "Version failed preflight checks" : preflight.blockingReason(),
                    requestUri
            );
            return Result.error(preflight.blockingReason() == null ? "Version failed preflight checks" : preflight.blockingReason());
        }

        version.setStatus(GameVersion.VersionStatus.SUBMITTED);
        version.setSubmitNote(note == null ? null : note.trim());
        version.setForcedUpdate(Boolean.TRUE.equals(forceUpdate));
        gameVersionRepository.save(version);

        game.setStatus(Game.GameStatus.PENDING);
        game.setVersion(version.getVersionName());
        game.setMd5(version.getMd5());
        game.setSourceMd5(version.getSourceMd5());
        game.setStorageKey(version.getStorageKey());
        game.setSourceStorageKey(version.getSourceStorageKey());
        game.setDownloadUrl(buildControlPlaneDownloadUrl(game.getAppId()));
        gameRepository.save(game);

        auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, game, true, "Submit specific version", requestUri);
        return Result.success();
    }

    public Result<Void> rollbackToVersion(
            Long gameId,
            Long versionId,
            User currentUser,
            String requestUri,
            String reason
    ) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            auditLogService.logGameAudit("GAME_ROLLBACK", currentUser, null, false, "Game not found", requestUri);
            return Result.error("Game not found");
        }

        boolean canOperate = currentUser.getRole() == User.UserRole.ADMIN
                || currentUser.getId().equals(game.getDeveloperId());
        if (!canOperate) {
            auditLogService.logGameAudit("GAME_ROLLBACK", currentUser, game, false, "Permission denied", requestUri);
            return Result.error("No permission to rollback this game");
        }

        GameVersion target = gameVersionRepository.findByIdAndGameId(versionId, gameId).orElse(null);
        if (target == null) {
            auditLogService.logGameAudit("GAME_ROLLBACK", currentUser, game, false, "Version not found", requestUri);
            return Result.error("Target version not found");
        }
        if (target.getStatus() != GameVersion.VersionStatus.APPROVED) {
            auditLogService.logGameAudit("GAME_ROLLBACK", currentUser, game, false, "Version not approved", requestUri);
            return Result.error("Only approved version can be used for rollback");
        }

        game.setStatus(Game.GameStatus.APPROVED);
        game.setVersion(target.getVersionName());
        game.setMd5(target.getMd5());
        game.setSourceMd5(target.getSourceMd5());
        game.setStorageKey(target.getStorageKey());
        game.setSourceStorageKey(target.getSourceStorageKey());
        game.setPackageFormat(target.getPackageFormat());
        game.setPackageKeyCiphertext(target.getPackageKeyCiphertext());
        game.setPackageKeyNonce(target.getPackageKeyNonce());
        game.setDownloadUrl(buildControlPlaneDownloadUrl(game.getAppId()));
        gameRepository.save(game);

        String finalReason = (reason == null || reason.isBlank()) ? "Rollback publish" : reason.trim();
        auditLogService.logGameAudit("GAME_ROLLBACK", currentUser, game, true, finalReason, requestUri);
        return Result.success();
    }

    public Result<Void> directPublishVersionAsAdmin(
            Long gameId,
            Long versionId,
            User currentUser,
            String requestUri,
            String reason,
            Boolean forceUpdate
    ) {
        if (gameId == null || gameId <= 0 || versionId == null || versionId <= 0) {
            return Result.error("Invalid game or version id");
        }
        if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
            auditLogService.logOpsAudit("OPS_GAME_VERSION_DIRECT_PUBLISH", currentUser, gameId, null, false, "Permission denied", requestUri);
            return Result.error("No permission to direct publish");
        }
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            auditLogService.logOpsAudit("OPS_GAME_VERSION_DIRECT_PUBLISH", currentUser, gameId, null, false, "Game not found", requestUri);
            return Result.error("Game not found");
        }
        GameVersion version = gameVersionRepository.findByIdAndGameId(versionId, gameId).orElse(null);
        if (version == null) {
            auditLogService.logOpsAudit("OPS_GAME_VERSION_DIRECT_PUBLISH", currentUser, gameId, game.getAppId(), false, "Game version not found", requestUri);
            return Result.error("Game version not found");
        }
        if (version.getStatus() == GameVersion.VersionStatus.PROCESSING) {
            auditLogService.logOpsAudit("OPS_GAME_VERSION_DIRECT_PUBLISH", currentUser, gameId, game.getAppId(), false, "Version package is still processing", requestUri);
            return Result.error("Version package is still processing");
        }
        if (gameVersionRepository.existsByGameIdAndStatus(game.getId(), GameVersion.VersionStatus.SUBMITTED)
                && version.getStatus() != GameVersion.VersionStatus.SUBMITTED) {
            auditLogService.logOpsAudit("OPS_GAME_VERSION_DIRECT_PUBLISH", currentUser, gameId, game.getAppId(), false, "Another version is already under review", requestUri);
            return Result.error("Another version is already under review");
        }

        String normalizedReason = trimToNull(reason, 256);
        if (normalizedReason == null) {
            normalizedReason = "Direct go-live from operations portal";
        } else if (normalizedReason.length() < 2) {
            return Result.error("Publish reason must be at least 2 chars");
        }

        boolean normalizedForceUpdate = Boolean.TRUE.equals(forceUpdate);
        DeveloperVersionPreflightDto preflight = buildDeveloperVersionPreflight(game, version, normalizedForceUpdate);
        if (!Boolean.TRUE.equals(preflight.ready())) {
            String blockingReason = defaultIfBlank(preflight.blockingReason(), "Version failed preflight checks");
            auditLogService.logOpsAudit("OPS_GAME_VERSION_DIRECT_PUBLISH", currentUser, gameId, game.getAppId(), false, blockingReason, requestUri);
            return Result.error(blockingReason);
        }

        version.setForcedUpdate(normalizedForceUpdate);
        version.setStatus(GameVersion.VersionStatus.APPROVED);
        version.setAuditReason(normalizedReason);
        version.setAssignedReviewerId(null);
        version.setAssignedAt(null);
        gameVersionRepository.save(version);

        game.setStatus(Game.GameStatus.APPROVED);
        game.setVersion(version.getVersionName());
        game.setMd5(version.getMd5());
        game.setSourceMd5(version.getSourceMd5());
        game.setStorageKey(version.getStorageKey());
        game.setSourceStorageKey(version.getSourceStorageKey());
        game.setPackageFormat(version.getPackageFormat());
        game.setPackageKeyCiphertext(version.getPackageKeyCiphertext());
        game.setPackageKeyNonce(version.getPackageKeyNonce());
        game.setDownloadUrl(buildControlPlaneDownloadUrl(game.getAppId()));
        gameRepository.save(game);

        auditLogService.logOpsAudit("OPS_GAME_VERSION_DIRECT_PUBLISH", currentUser, game.getId(), game.getAppId(), true, normalizedReason, requestUri);
        return Result.success();
    }

    public Result<Void> approveGame(Long id, User currentUser, String requestUri, String reason) {
        if (reason == null || reason.trim().length() < 2) {
            return Result.error("Audit reason must be at least 2 chars");
        }

        Game game = gameRepository.findById(id).orElse(null);
        if (game == null) {
            auditLogService.logGameAudit("GAME_APPROVE", currentUser, null, false, "Game not found", requestUri);
            return Result.error("Game not found");
        }

        GameVersion submitted = gameVersionRepository
                .findTopByGameIdAndStatusOrderByCreatedAtDesc(game.getId(), GameVersion.VersionStatus.SUBMITTED)
                .orElse(null);
        if (submitted == null || game.getStatus() != Game.GameStatus.PENDING) {
            auditLogService.logGameAudit("GAME_APPROVE", currentUser, game, false, "Invalid status transition", requestUri);
            return Result.error("Current status does not allow approve");
        }

        submitted.setStatus(GameVersion.VersionStatus.APPROVED);
        submitted.setAuditReason(reason.trim());
        gameVersionRepository.save(submitted);

        game.setStatus(Game.GameStatus.APPROVED);
        game.setVersion(submitted.getVersionName());
        game.setMd5(submitted.getMd5());
        game.setSourceMd5(submitted.getSourceMd5());
        game.setStorageKey(submitted.getStorageKey());
        game.setSourceStorageKey(submitted.getSourceStorageKey());
        game.setPackageFormat(submitted.getPackageFormat());
        game.setPackageKeyCiphertext(submitted.getPackageKeyCiphertext());
        game.setPackageKeyNonce(submitted.getPackageKeyNonce());
        game.setDownloadUrl(buildControlPlaneDownloadUrl(game.getAppId()));
        gameRepository.save(game);

        auditLogService.logGameAudit("GAME_APPROVE", currentUser, game, true, reason.trim(), requestUri);
        return Result.success();
    }

    public Result<Void> rejectGame(Long id, User currentUser, String requestUri, String reason) {
        if (reason == null || reason.trim().length() < 2) {
            return Result.error("Reject reason must be at least 2 chars");
        }

        Game game = gameRepository.findById(id).orElse(null);
        if (game == null) {
            auditLogService.logGameAudit("GAME_REJECT", currentUser, null, false, "Game not found", requestUri);
            return Result.error("Game not found");
        }

        GameVersion submitted = gameVersionRepository
                .findTopByGameIdAndStatusOrderByCreatedAtDesc(game.getId(), GameVersion.VersionStatus.SUBMITTED)
                .orElse(null);
        if (submitted == null || game.getStatus() != Game.GameStatus.PENDING) {
            auditLogService.logGameAudit("GAME_REJECT", currentUser, game, false, "Invalid status transition", requestUri);
            return Result.error("Current status does not allow reject");
        }

        submitted.setStatus(GameVersion.VersionStatus.REJECTED);
        submitted.setAuditReason(reason.trim());
        gameVersionRepository.save(submitted);

        game.setStatus(Game.GameStatus.REJECTED);
        gameRepository.save(game);

        auditLogService.logGameAudit("GAME_REJECT", currentUser, game, true, reason.trim(), requestUri);
        return Result.success();
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    private String validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "Uploaded file is empty";
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            return "Only zip upload is supported";
        }
        if (originalName.length() > 128) {
            return "Zip filename is too long";
        }
        if (file.getSize() > MAX_UPLOAD_SIZE_BYTES) {
            return "Zip file exceeds size limit";
        }

        try {
            byte[] zipBytes = file.getBytes();
            hostedMiniAppPackageService.inspect(zipBytes);

            try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            int entries = 0;
            long totalSize = 0L;
            boolean hasIndexHtml = false;
            byte[] buffer = new byte[8192];
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                entries++;
                if (entries > 1000) {
                    return "Zip contains too many files";
                }
                String name = entry.getName();
                if (name == null || name.isBlank() || name.contains("..") || name.startsWith("/") || name.startsWith("\\")) {
                    return "Zip contains invalid file path";
                }
                if ("index.html".equalsIgnoreCase(name)) {
                    hasIndexHtml = true;
                }

                int read;
                while ((read = zis.read(buffer)) != -1) {
                    totalSize += read;
                    if (totalSize > 200L * 1024 * 1024) {
                        return "Unzipped package exceeds size limit";
                    }
                }
            }

            if (!hasIndexHtml) {
                return "Zip missing index.html";
            }
            }
        } catch (Exception e) {
            return "Zip validation failed: " + e.getMessage();
        }
        return null;
    }

    public void attachHostedManifestReport(GameVersion version) {
        version.setHostedManifestValid(Boolean.FALSE);
        version.setHostedManifestSummary("Manifest inspection unavailable");

        String sourceStorageKey = version.getSourceStorageKey();
        if (sourceStorageKey == null || sourceStorageKey.isBlank()) {
            version.setHostedManifestSummary("Source package is missing");
            return;
        }

        try (GetObjectResponse stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(sourceStorageKey)
                        .build())) {
            HostedMiniAppPackageService.HostedMiniAppManifestReport report =
                    hostedMiniAppPackageService.inspectReport(stream.readAllBytes());
            version.setHostedManifestValid(Boolean.TRUE);
            version.setHostedManifestEntry(report.entryFile());
            version.setHostedManifestKind(report.kind());
            version.setHostedManifestSummary(buildHostedManifestSummary(report));
        } catch (Exception exception) {
            version.setHostedManifestSummary("Manifest inspection failed: " + exception.getMessage());
        }
    }

    private String buildHostedManifestSummary(HostedMiniAppPackageService.HostedMiniAppManifestReport report) {
        List<String> parts = new ArrayList<>();
        if (report.appId() != null && !report.appId().isBlank()) {
            parts.add("appId=" + report.appId());
        }
        if (report.version() != null && !report.version().isBlank()) {
            parts.add("version=" + report.version());
        }
        parts.add("entry=" + report.entryFile());
        parts.add("kind=" + report.kind());
        return String.join(", ", parts);
    }

    private String normalizePresignedUrlForClient(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return rawUrl;
        }
        try {
            URI presigned = URI.create(rawUrl);
            String presignedHost = presigned.getHost();
            if (!isLoopbackHost(presignedHost)) {
                return rawUrl;
            }

            URI publicUri = URI.create(publicBaseUrl);
            String clientHost = publicUri.getHost();
            if (clientHost == null || clientHost.isBlank()) {
                return rawUrl;
            }

            String scheme = publicUri.getScheme() == null ? presigned.getScheme() : publicUri.getScheme();
            URI normalized = new URI(
                    scheme,
                    presigned.getUserInfo(),
                    clientHost,
                    presigned.getPort(),
                    presigned.getPath(),
                    presigned.getQuery(),
                    presigned.getFragment()
            );
            return normalized.toString();
        } catch (Exception ignored) {
            return rawUrl;
        }
    }

    private boolean isLoopbackHost(String host) {
        if (host == null) {
            return false;
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        return "localhost".equals(normalized)
                || "127.0.0.1".equals(normalized)
                || "::1".equals(normalized);
    }

    private String buildControlPlaneDownloadUrl(String appId) {
        return publicBaseUrl.replaceAll("/+$", "") + "/game/download/" + appId;
    }

    private String normalizeVersionNameForUpload(String versionName) {
        String normalized = trimToNull(versionName, 64);
        if (normalized == null) {
            return null;
        }
        if (!normalized.matches("[0-9A-Za-z._-]{1,64}")) {
            return null;
        }
        return normalized;
    }

    private String buildNextVersionName(Long gameId) {
        long versionCount = gameVersionRepository.countByGameId(gameId);
        return "1.0." + versionCount;
    }

    private String generateAppId() {
        return "wx" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private int compareVersion(String left, String right) {
        String[] l = left.split("\\.");
        String[] r = right.split("\\.");
        int size = Math.max(l.length, r.length);
        for (int i = 0; i < size; i++) {
            int lv = i < l.length ? parseVersionPart(l[i]) : 0;
            int rv = i < r.length ? parseVersionPart(r[i]) : 0;
            if (lv != rv) {
                return lv - rv;
            }
        }
        return 0;
    }

    public String deriveFrontendState(Game game) {
        if (game == null || game.getStatus() == null) {
            return "UNKNOWN";
        }
        if (game.getStatus() == Game.GameStatus.APPROVED) {
            String visibilityStatus = effectiveVisibilityStatus(game);
            if ("BLOCKED".equals(visibilityStatus)) {
                return "BLOCKED";
            }
            if ("HIDDEN".equals(visibilityStatus)) {
                return "HIDDEN";
            }
            return "OPERABLE";
        }
        return switch (game.getStatus()) {
            case PENDING -> "UNDER_REVIEW";
            case DRAFT -> "NOT_OPEN";
            case REJECTED -> "BLOCKED";
            case PROCESSING -> "PROCESSING";
            case APPROVED -> "OPERABLE";
        };
    }

    public String effectiveVisibilityStatus(Game game) {
        String raw = normalizeVisibilityStatus(game == null ? null : game.getVisibilityStatus());
        if (raw == null) {
            return "VISIBLE";
        }
        if ("BLOCKED".equals(raw) && game != null && game.getVisibilityUntil() != null
                && game.getVisibilityUntil().isBefore(LocalDateTime.now())) {
            return "VISIBLE";
        }
        return raw;
    }

    public boolean isFrontendVisibleForPublic(Game game) {
        return game != null
                && game.getStatus() == Game.GameStatus.APPROVED
                && "VISIBLE".equals(effectiveVisibilityStatus(game));
    }

    private boolean canBypassFrontendVisibility(User currentUser, Game game) {
        return currentUser != null
                && (currentUser.getRole() == User.UserRole.ADMIN
                || (game != null && currentUser.getId().equals(game.getDeveloperId())));
    }

    private String normalizeVisibilityStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return "VISIBLE";
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        return Set.of("VISIBLE", "HIDDEN", "BLOCKED").contains(value) ? value : null;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String normalizeCategory(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.trim();
        if (normalized.isBlank()) {
            return "";
        }
        if ("all".equalsIgnoreCase(normalized)) {
            return "all";
        }
        ensureDefaultGameCategorySeed();
        Set<String> allowed = gameCategoryRepository.findByStatusOrderBySortOrderAscUpdatedAtDesc(CATEGORY_STATUS_ENABLED)
                .stream()
                .map(OpsGameCategory::getName)
                .collect(Collectors.toSet());
        return allowed.stream()
                .filter(name -> name.equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }

    private String trimToNull(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private String writeTagsJson(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return tags.stream()
                .map(tag -> trimToNull(tag, 32))
                .filter(tag -> tag != null && !tag.isBlank())
                .distinct()
                .limit(20)
                .collect(Collectors.joining(","));
    }

    private List<String> listEnabledGameCategoryNames() {
        ensureDefaultGameCategorySeed();
        return gameCategoryRepository.findByStatusOrderBySortOrderAscUpdatedAtDesc(CATEGORY_STATUS_ENABLED)
                .stream()
                .map(OpsGameCategory::getName)
                .toList();
    }

    private void ensureDefaultGameCategorySeed() {
        for (int i = 0; i < DEFAULT_GAME_CATEGORIES.size(); i++) {
            String name = DEFAULT_GAME_CATEGORIES.get(i);
            if (!gameCategoryRepository.existsByNameIgnoreCase(name)) {
                OpsGameCategory row = new OpsGameCategory();
                row.setName(name);
                row.setSortOrder((i + 1) * 10);
                row.setStatus(CATEGORY_STATUS_ENABLED);
                gameCategoryRepository.save(row);
            }
        }
    }

    private OpsGameCategoryResponse toGameCategoryResponse(OpsGameCategory row) {
        return new OpsGameCategoryResponse(row.getId(), row.getName(), row.getSortOrder());
    }

    private Integer categorySortOrder(Integer inputSortOrder) {
        return inputSortOrder == null ? 0 : Math.max(0, inputSortOrder);
    }

    private String validateSubmitNote(String note) {
        if (note == null) {
            return null;
        }
        String normalized = note.trim();
        if (normalized.length() > MAX_SUBMIT_NOTE_LENGTH) {
            return "Submit note is too long";
        }
        return null;
    }

    private DeveloperReleaseHealthDto.GameReleaseItem toDeveloperReleaseItem(Game game) {
        List<GameVersion> versions = gameVersionRepository.findByGameIdOrderByCreatedAtDesc(game.getId());
        GameVersion latest = versions.isEmpty() ? null : versions.get(0);
        if (latest != null) {
            attachHostedManifestReport(latest);
        }
        boolean hasSubmitted = versions.stream().anyMatch(version -> version.getStatus() == GameVersion.VersionStatus.SUBMITTED);
        boolean hasApproved = versions.stream().anyMatch(version -> version.getStatus() == GameVersion.VersionStatus.APPROVED);
        boolean canSubmit = latest != null
                && (latest.getStatus() == GameVersion.VersionStatus.DRAFT || latest.getStatus() == GameVersion.VersionStatus.REJECTED)
                && !hasSubmitted
                && game.getStatus() != Game.GameStatus.PROCESSING
                && Boolean.TRUE.equals(latest.getHostedManifestValid());
        boolean canRollback = hasApproved;
        String blockingReason = deriveDeveloperBlockingReason(game, latest, hasSubmitted);
        LocalDateTime lastUpdated = latest == null ? game.getUpdatedAt() : (latest.getUpdatedAt() == null ? latest.getCreatedAt() : latest.getUpdatedAt());
        return new DeveloperReleaseHealthDto.GameReleaseItem(
                game.getId(),
                game.getAppId(),
                game.getName(),
                game.getStatus() == null ? "" : game.getStatus().name(),
                effectiveVisibilityStatus(game),
                deriveFrontendState(game),
                latest == null ? null : latest.getVersionName(),
                latest == null || latest.getStatus() == null ? null : latest.getStatus().name(),
                latest == null ? null : latest.getHostedManifestValid(),
                canSubmit,
                canRollback,
                blockingReason,
                lastUpdated
        );
    }

    private DeveloperUploadTaskDto toDeveloperUploadTask(Game game) {
        GameVersion latest = game.getId() == null ? null : gameVersionRepository.findTopByGameIdOrderByCreatedAtDesc(game.getId()).orElse(null);
        if (latest != null) {
            attachHostedManifestReport(latest);
        }
        String taskStatus;
        if (game.getStatus() == Game.GameStatus.PROCESSING) {
            taskStatus = "PROCESSING";
        } else if (game.getUploadProcessingFailureReason() != null && !game.getUploadProcessingFailureReason().isBlank()) {
            taskStatus = "FAILED";
        } else {
            taskStatus = "SUCCEEDED";
        }
        return new DeveloperUploadTaskDto(
                game.getId(),
                game.getAppId(),
                game.getName(),
                game.getUploadFileName(),
                game.getUploadFileSizeBytes(),
                taskStatus,
                game.getStatus() == null ? null : game.getStatus().name(),
                latest == null ? game.getVersion() : latest.getVersionName(),
                game.getUploadProcessingFailureReason(),
                game.getUploadProcessingRetryCount(),
                latest == null ? null : latest.getHostedManifestValid(),
                latest == null ? null : latest.getHostedManifestSummary(),
                game.getUploadProcessingStartedAt(),
                game.getUploadProcessingFinishedAt(),
                game.getCreatedAt(),
                game.getUpdatedAt()
        );
    }

    private DeveloperVersionPreflightDto buildDeveloperVersionPreflight(Game game, GameVersion version, Boolean forceUpdate) {
        attachHostedManifestReport(version);
        boolean processing = game.getStatus() == Game.GameStatus.PROCESSING;
        boolean validStatus = version.getStatus() == GameVersion.VersionStatus.DRAFT
                || version.getStatus() == GameVersion.VersionStatus.REJECTED;
        boolean reviewQueueAvailable = !gameVersionRepository.existsByGameIdAndStatus(game.getId(), GameVersion.VersionStatus.SUBMITTED);
        boolean manifestValid = Boolean.TRUE.equals(version.getHostedManifestValid());
        boolean duplicatePackageDetected = version.getSourceMd5() != null
                && !version.getSourceMd5().isBlank()
                && gameVersionRepository.existsByGameIdAndSourceMd5AndIdNot(game.getId(), version.getSourceMd5(), version.getId());
        boolean duplicateVersionNameDetected = version.getVersionName() != null
                && gameVersionRepository.findByGameIdOrderByCreatedAtDesc(game.getId()).stream()
                .filter(row -> !row.getId().equals(version.getId()))
                .filter(row -> row.getVersionName() != null)
                .anyMatch(row -> row.getVersionName().equalsIgnoreCase(version.getVersionName())
                        && row.getStatus() != GameVersion.VersionStatus.REJECTED);
        boolean forceUpdateAllowed = !Boolean.TRUE.equals(forceUpdate)
                || gameVersionRepository.findTopByGameIdAndStatusOrderByCreatedAtDesc(game.getId(), GameVersion.VersionStatus.APPROVED).isPresent();

        List<DeveloperVersionPreflightDto.PreflightItem> items = List.of(
                new DeveloperVersionPreflightDto.PreflightItem(
                        "PACKAGE_READY",
                        "Source package processed",
                        !processing,
                        true,
                        processing ? "Game package is still processing" : "Package processing finished"
                ),
                new DeveloperVersionPreflightDto.PreflightItem(
                        "VERSION_STATUS",
                        "Version state allows submit",
                        validStatus,
                        true,
                        validStatus ? "Current version can enter review" : "Current version status does not allow submit"
                ),
                new DeveloperVersionPreflightDto.PreflightItem(
                        "MANIFEST_VALID",
                        "Hosted manifest validation",
                        manifestValid,
                        true,
                        manifestValid ? defaultIfBlank(version.getHostedManifestSummary(), "Manifest validation passed")
                                : defaultIfBlank(version.getHostedManifestSummary(), "Hosted package manifest is invalid")
                ),
                new DeveloperVersionPreflightDto.PreflightItem(
                        "REVIEW_QUEUE",
                        "Review queue availability",
                        reviewQueueAvailable,
                        true,
                        reviewQueueAvailable ? "No other submitted version is occupying the queue"
                                : "Another version is already under review"
                ),
                new DeveloperVersionPreflightDto.PreflightItem(
                        "DUPLICATE_PACKAGE",
                        "Duplicate package detection",
                        !duplicatePackageDetected,
                        true,
                        duplicatePackageDetected ? "Another version already uses the same uploaded package"
                                : "No duplicate package detected"
                ),
                new DeveloperVersionPreflightDto.PreflightItem(
                        "DUPLICATE_VERSION_NAME",
                        "Duplicate version name detection",
                        !duplicateVersionNameDetected,
                        true,
                        duplicateVersionNameDetected ? "Another active version already uses the same version name"
                                : "No duplicate version name detected"
                ),
                new DeveloperVersionPreflightDto.PreflightItem(
                        "FORCE_UPDATE",
                        "Force update readiness",
                        forceUpdateAllowed,
                        true,
                        forceUpdateAllowed ? "Force update policy is valid"
                                : "First release cannot be force update"
                )
        );

        String blockingReason = items.stream()
                .filter(item -> Boolean.FALSE.equals(item.passed()) && Boolean.TRUE.equals(item.blocking()))
                .map(DeveloperVersionPreflightDto.PreflightItem::detail)
                .findFirst()
                .orElse(null);

        return new DeveloperVersionPreflightDto(
                game.getId(),
                version.getId(),
                version.getVersionName(),
                blockingReason == null,
                duplicatePackageDetected,
                duplicateVersionNameDetected,
                manifestValid,
                reviewQueueAvailable,
                forceUpdateAllowed,
                blockingReason,
                items
        );
    }

    private List<DeveloperMetricTrendPointDto> buildMetricTrend(List<DeveloperGameMetricDaily> metrics, LocalDate fromDate) {
        Map<LocalDate, List<DeveloperGameMetricDaily>> bucket = metrics.stream()
                .collect(Collectors.groupingBy(DeveloperGameMetricDaily::getMetricDate, LinkedHashMap::new, Collectors.toList()));
        List<DeveloperMetricTrendPointDto> rows = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = fromDate.plusDays(i);
            List<DeveloperGameMetricDaily> dailyRows = bucket.getOrDefault(date, List.of());
            BigDecimal avgSession = dailyRows.isEmpty()
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(dailyRows.stream()
                    .map(item -> item.getAvgSessionMinutes() == null ? BigDecimal.ZERO : item.getAvgSessionMinutes())
                    .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue() / dailyRows.size()).setScale(2, java.math.RoundingMode.HALF_UP);
            rows.add(new DeveloperMetricTrendPointDto(
                    date,
                    dailyRows.stream().mapToInt(item -> safeInt(item.getInstalls())).sum(),
                    dailyRows.stream().mapToInt(item -> safeInt(item.getLaunches())).sum(),
                    dailyRows.stream().mapToInt(item -> safeInt(item.getActiveUsers())).sum(),
                    dailyRows.stream().mapToInt(item -> safeInt(item.getInstallFailures())).sum(),
                    dailyRows.stream().mapToInt(item -> safeInt(item.getLaunchFailures())).sum(),
                    avgSession
            ));
        }
        return rows;
    }

    private List<DeveloperOperationsDashboardDto.GameOpsRow> buildGameOpsRows(
            List<DeveloperGameMetricDaily> metrics,
            List<DeveloperRuntimeIssueDto> runtimeIssues,
            Map<Long, Game> gameMap
    ) {
        Map<Long, List<DeveloperGameMetricDaily>> metricBucket = metrics.stream()
                .collect(Collectors.groupingBy(DeveloperGameMetricDaily::getGameId));
        Map<Long, Long> issueBucket = runtimeIssues.stream()
                .filter(item -> !"RESOLVED".equalsIgnoreCase(item.issueStatus()))
                .collect(Collectors.groupingBy(DeveloperRuntimeIssueDto::gameId, Collectors.counting()));
        return gameMap.values().stream()
                .map(game -> {
                    List<DeveloperGameMetricDaily> rows = metricBucket.getOrDefault(game.getId(), List.of());
                    BigDecimal avgSession = rows.isEmpty()
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(rows.stream()
                            .map(item -> item.getAvgSessionMinutes() == null ? BigDecimal.ZERO : item.getAvgSessionMinutes())
                            .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue() / rows.size()).setScale(2, java.math.RoundingMode.HALF_UP);
                    return new DeveloperOperationsDashboardDto.GameOpsRow(
                            game.getId(),
                            game.getAppId(),
                            game.getName(),
                            rows.stream().mapToInt(item -> safeInt(item.getInstalls())).sum(),
                            rows.stream().mapToInt(item -> safeInt(item.getLaunches())).sum(),
                            rows.stream().mapToInt(item -> safeInt(item.getActiveUsers())).sum(),
                            rows.stream().mapToInt(item -> safeInt(item.getInstallFailures())).sum(),
                            rows.stream().mapToInt(item -> safeInt(item.getLaunchFailures())).sum(),
                            avgSession,
                            issueBucket.getOrDefault(game.getId(), 0L).intValue()
                    );
                })
                .toList();
    }

    private DeveloperRuntimeIssueDto toRuntimeIssueDto(DeveloperRuntimeIssue issue, Game game) {
        return new DeveloperRuntimeIssueDto(
                issue.getId(),
                issue.getGameId(),
                issue.getAppId(),
                game == null ? issue.getAppId() : game.getName(),
                issue.getIssueType(),
                issue.getSeverity(),
                issue.getIssueCode(),
                issue.getIssueMessage(),
                issue.getImpactedUsers(),
                issue.getImpactedDevices(),
                issue.getIssueStatus(),
                issue.getFirstOccurredAt(),
                issue.getLastOccurredAt()
        );
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String deriveDeveloperBlockingReason(Game game, GameVersion latest, boolean hasSubmittedVersion) {
        if (game == null) {
            return "Game not found";
        }
        if (game.getStatus() == Game.GameStatus.PROCESSING) {
            return "Package is still processing";
        }
        if (!"VISIBLE".equals(effectiveVisibilityStatus(game))) {
            return "Blocked by frontend visibility control";
        }
        if (latest == null) {
            return "No version available";
        }
        if (hasSubmittedVersion) {
            return "A version is already under review";
        }
        if (!Boolean.TRUE.equals(latest.getHostedManifestValid())) {
            return "Hosted package manifest is invalid";
        }
        if (latest.getStatus() == GameVersion.VersionStatus.REJECTED) {
            return "Latest version was rejected and needs fixes";
        }
        if (latest.getStatus() == GameVersion.VersionStatus.APPROVED) {
            return null;
        }
        if (latest.getStatus() == GameVersion.VersionStatus.DRAFT) {
            return null;
        }
        return "Current version state does not allow release action";
    }

    private String normalizeCategoryName(String rawName) {
        if (rawName == null) {
            return null;
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private ResolvedPackageContext resolvePackageContext(String appId, String version) {
        Game game = gameRepository.findByAppId(appId);
        if (game == null || game.getStatus() != Game.GameStatus.APPROVED) {
            return ResolvedPackageContext.error("Game not found");
        }

        String resolvedVersion = version == null ? "" : version.trim();
        GameVersion resolved = resolvedVersion.isBlank()
                ? gameVersionRepository.findTopByGameIdAndStatusOrderByCreatedAtDesc(
                        game.getId(),
                        GameVersion.VersionStatus.APPROVED
                ).orElse(null)
                : gameVersionRepository.findTopByGameIdAndVersionNameAndStatusOrderByCreatedAtDesc(
                        game.getId(),
                        resolvedVersion,
                        GameVersion.VersionStatus.APPROVED
                ).orElse(null);

        String wrappedKey = resolved == null ? game.getPackageKeyCiphertext() : resolved.getPackageKeyCiphertext();
        String wrappedNonce = resolved == null ? game.getPackageKeyNonce() : resolved.getPackageKeyNonce();
        String packageFormat = resolved == null ? game.getPackageFormat() : resolved.getPackageFormat();
        String keyVersion = resolved == null ? game.getVersion() : resolved.getVersionName();

        if (wrappedKey == null || wrappedKey.isBlank() || wrappedNonce == null || wrappedNonce.isBlank()) {
            return ResolvedPackageContext.error("Game package key is unavailable");
        }

        return new ResolvedPackageContext(
                game,
                keyVersion == null ? "" : keyVersion,
                packageFormat == null ? "" : packageFormat,
                wrappedKey,
                wrappedNonce,
                null
        );
    }

    private String runtimeTicketKey(String ticket) {
        return "game:runtime-ticket:" + ticket;
    }

    private String signRuntimeTicket(Map<String, Object> payload) throws Exception {
        String payloadJson = objectMapper.writeValueAsString(payload);
        String payloadPart = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signaturePart = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(hmacSha256(payloadPart));
        return payloadPart + "." + signaturePart;
    }

    private Map<String, Object> parseAndVerifyRuntimeTicket(String ticket) throws Exception {
        String[] parts = ticket.split("\\.");
        if (parts.length != 2) {
            throw new IllegalStateException("Malformed runtime ticket");
        }
        String payloadPart = parts[0];
        String signaturePart = parts[1];
        byte[] expected = hmacSha256(payloadPart);
        byte[] actual = Base64.getUrlDecoder().decode(signaturePart);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new IllegalStateException("Invalid runtime ticket signature");
        }
        byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadPart);
        return objectMapper.readValue(payloadBytes, new TypeReference<>() {});
    }

    private byte[] hmacSha256(String input) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                gamePackageProperties.getRuntimeTicketSigningKey().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));
        return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
    }

    private long parseLongValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignore) {
            return 0L;
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Set<String> parseSupportedFormats(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return Set.of();
        }
        Set<String> output = new HashSet<>();
        for (String token : headerValue.split(",")) {
            String normalized = token == null ? "" : token.trim();
            if (normalized.isBlank()) {
                continue;
            }
            output.add(normalized);
        }
        return output;
    }

    private void normalizeClientUrls(Game game) {
        if (game == null || game.getAppId() == null) {
            return;
        }
        if (game.getId() != null) {
            gameVersionRepository
                    .findTopByGameIdAndStatusOrderByCreatedAtDesc(game.getId(), GameVersion.VersionStatus.APPROVED)
                    .ifPresent(latestApproved -> {
                        if (latestApproved.getVersionName() != null && !latestApproved.getVersionName().isBlank()) {
                            game.setVersion(latestApproved.getVersionName());
                        }
                        if (latestApproved.getMd5() != null && !latestApproved.getMd5().isBlank()) {
                            game.setMd5(latestApproved.getMd5());
                        }
                    });
        }
        if (game.getRequiresOnline() == null) {
            game.setRequiresOnline(false);
        }
        String downloadUrl = game.getDownloadUrl();
        if (downloadUrl == null || downloadUrl.isBlank()
                || downloadUrl.contains("/game/download-url/")
                || downloadUrl.endsWith("/game/download/" + game.getAppId())) {
            game.setDownloadUrl(buildControlPlaneDownloadUrl(game.getAppId()));
        }
        attachHostedLocales(game);
    }

    private void attachHostedLocales(Game game) {
        if (game == null || game.getId() == null) {
            return;
        }
        try {
            GameVersion latestApproved = gameVersionRepository
                    .findTopByGameIdAndStatusOrderByCreatedAtDesc(game.getId(), GameVersion.VersionStatus.APPROVED)
                    .orElse(null);
            String sourceStorageKey = latestApproved != null && latestApproved.getSourceStorageKey() != null
                    && !latestApproved.getSourceStorageKey().isBlank()
                    ? latestApproved.getSourceStorageKey()
                    : game.getSourceStorageKey();
            if (sourceStorageKey == null || sourceStorageKey.isBlank()) {
                return;
            }
            HostedMiniAppPackageService.HostedMiniAppManifestReport report;
            try (GetObjectResponse stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(sourceStorageKey)
                            .build())) {
                report = hostedMiniAppPackageService.inspectReport(stream.readAllBytes());
            }
            Map<String, HostedMiniAppPackageService.HostedMiniAppLocale> locales = report.locales();
            if (locales == null || locales.isEmpty()) {
                return;
            }
            Map<String, Map<String, String>> payload = new LinkedHashMap<>();
            for (Map.Entry<String, HostedMiniAppPackageService.HostedMiniAppLocale> entry : locales.entrySet()) {
                HostedMiniAppPackageService.HostedMiniAppLocale locale = entry.getValue();
                Map<String, String> value = new LinkedHashMap<>();
                if (locale.name() != null && !locale.name().isBlank()) {
                    value.put("name", locale.name());
                }
                if (locale.description() != null && !locale.description().isBlank()) {
                    value.put("description", locale.description());
                }
                if (!value.isEmpty()) {
                    payload.put(entry.getKey(), value);
                }
            }
            game.setLocales(payload.isEmpty() ? Collections.emptyMap() : payload);
        } catch (Exception ignored) {
            // Keep database fields as the fallback when legacy source packages have no locale metadata.
        }
    }

    private record ResolvedPackageContext(
            Game game,
            String version,
            String packageFormat,
            String wrappedKey,
            String wrappedNonce,
            String error
    ) {
        private static ResolvedPackageContext error(String message) {
            return new ResolvedPackageContext(null, "", "", "", "", message);
        }
    }
}
