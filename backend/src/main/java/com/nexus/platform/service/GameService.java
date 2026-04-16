package com.nexus.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.config.GamePackageProperties;
import com.nexus.platform.dto.GameUpdateCheckResponse;
import com.nexus.platform.dto.GameMetadataUpdateRequest;
import com.nexus.platform.dto.OpsGameCategoryRequest;
import com.nexus.platform.dto.OpsGameCategoryResponse;
import com.nexus.platform.dto.PageResult;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.GameVersion;
import com.nexus.platform.entity.OpsGameCategory;
import com.nexus.platform.entity.User;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
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

    public record GameDownloadStream(GetObjectResponse stream, String filename) {
    }

    public record RuntimePackageKey(String key, String algorithm, String version, String format) {
    }

    public record RuntimePackageTicket(String ticket, long expiresInSeconds, String version, String format) {
    }

    private final GameRepository gameRepository;
    private final GameVersionRepository gameVersionRepository;
    private final OpsGameCategoryRepository gameCategoryRepository;
    private final MinioClient minioClient;
    private final AuditLogService auditLogService;
    private final UploadProcessingService uploadProcessingService;
    private final SecureGamePackageService secureGamePackageService;
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
            game.setStatus(Game.GameStatus.PROCESSING);
            game.setDeveloperId(currentUser.getId());
            game.setDownloadUrl(buildControlPlaneDownloadUrl(appId));
            game = gameRepository.save(game);

            uploadProcessingService.processUpload(game.getId());
            return Result.success(game);
        } catch (Exception e) {
            return Result.error("Upload failed: " + e.getMessage());
        }
    }

    public Result<GameUpdateCheckResponse> checkUpdate(String appId, String localVersion) {
        Game game = gameRepository.findByAppId(appId);
        if (game == null || game.getStatus() != Game.GameStatus.APPROVED) {
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
        List<Game> games = gameRepository.findByStatusOrderByCreatedAtDesc(Game.GameStatus.APPROVED);
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
        gamesPage.getContent().forEach(this::normalizeClientUrls);
        return Result.success(new PageResult<>(
                gamesPage.getContent(),
                gamesPage.getNumber(),
                gamesPage.getSize(),
                gamesPage.getTotalElements(),
                gamesPage.getTotalPages()
        ));
    }

    public Result<Game> getGameByAppId(String appId) {
        Game game = gameRepository.findByAppId(appId);
        if (game == null) {
            return Result.error("Game not found");
        }
        normalizeClientUrls(game);
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
        if (request == null) {
            return Result.error("Request body is required");
        }
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            return Result.error("Game not found");
        }
        boolean canEdit = currentUser != null && (currentUser.getRole() == User.UserRole.ADMIN
                || currentUser.getId().equals(game.getDeveloperId()));
        if (!canEdit) {
            return Result.error("No permission to edit this game");
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
            String serialized = request.tags().stream()
                    .map(tag -> tag == null ? "" : tag.trim())
                    .filter(tag -> !tag.isBlank())
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

    public Result<String> getPresignedDownloadUrl(String appId, User currentUser) {
        try {
            Game game = gameRepository.findByAppId(appId);
            if (game == null) {
                return Result.error("Game not found");
            }

            boolean canDownload = game.getStatus() == Game.GameStatus.APPROVED
                    || (currentUser != null && (currentUser.getRole() == User.UserRole.ADMIN
                    || currentUser.getId().equals(game.getDeveloperId())));
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
        return Result.success(gameVersionRepository.findByGameIdOrderByCreatedAtDesc(gameId));
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

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
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
        } catch (Exception e) {
            return "Zip validation failed: " + e.getMessage();
        }
        return null;
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
        if (game.getRequiresOnline() == null) {
            game.setRequiresOnline(false);
        }
        String downloadUrl = game.getDownloadUrl();
        if (downloadUrl == null || downloadUrl.isBlank()
                || downloadUrl.contains("/game/download-url/")
                || downloadUrl.endsWith("/game/download/" + game.getAppId())) {
            game.setDownloadUrl(buildControlPlaneDownloadUrl(game.getAppId()));
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
