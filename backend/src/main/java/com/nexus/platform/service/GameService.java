package com.nexus.platform.service;

import com.nexus.platform.dto.PageResult;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.GameVersion;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.GameVersionRepository;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;
    private final GameVersionRepository gameVersionRepository;
    private final MinioClient minioClient;
    private final AuditLogService auditLogService;
    private final UploadProcessingService uploadProcessingService;

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
            String storageKey = "games/" + appId + ".zip";
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType("application/zip")
                            .build()
            );

            Game game = new Game();
            game.setAppId(appId);
            game.setName(name);
            game.setDescription(description);
            game.setStorageKey(storageKey);
            game.setStatus(Game.GameStatus.PROCESSING);
            game.setDeveloperId(currentUser.getId());
            game.setDownloadUrl(buildControlPlaneDownloadUrl(appId));
            game = gameRepository.save(game);

            uploadProcessingService.processUpload(game.getId());
            return Result.success(game);
        } catch (Exception e) {
            return Result.error("涓婁紶澶辫触: " + e.getMessage());
        }
    }

    public Result<List<Game>> getGameList(User currentUser) {
        if (currentUser != null && currentUser.getRole() == User.UserRole.ADMIN) {
            List<Game> games = gameRepository.findAllByOrderByCreatedAtDesc();
            games.forEach(this::normalizeClientUrls);
            return Result.success(games);
        }
        List<Game> games = gameRepository.findByStatus(Game.GameStatus.APPROVED);
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
            return Result.error("娓告垙涓嶅瓨鍦?);
        }
        normalizeClientUrls(game);
        return Result.success(game);
    }

    public Result<List<Game>> getDeveloperGames(Long developerId, User currentUser) {
        if (currentUser == null) {
            return Result.error("缂哄皯鏈夋晥鐨勭櫥褰曞嚟璇?);
        }
        if (currentUser.getRole() != User.UserRole.ADMIN && !currentUser.getId().equals(developerId)) {
            return Result.error("鏃犳潈鏌ョ湅鍏朵粬寮€鍙戣€呯殑娓告垙鍒楄〃");
        }
        List<Game> games = gameRepository.findByDeveloperIdOrderByCreatedAtDesc(developerId);
        games.forEach(this::normalizeClientUrls);
        return Result.success(games);
    }

    public Result<PageResult<Game>> getDeveloperGamesPaged(Long developerId, User currentUser, int page, int size) {
        if (currentUser == null) {
            return Result.error("缂哄皯鏈夋晥鐨勭櫥褰曞嚟璇?);
        }
        if (currentUser.getRole() != User.UserRole.ADMIN && !currentUser.getId().equals(developerId)) {
            return Result.error("鏃犳潈鏌ョ湅鍏朵粬寮€鍙戣€呯殑娓告垙鍒楄〃");
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
                return Result.error("娓告垙涓嶅瓨鍦?);
            }

            boolean canDownload = game.getStatus() == Game.GameStatus.APPROVED
                    || (currentUser != null && (currentUser.getRole() == User.UserRole.ADMIN
                    || currentUser.getId().equals(game.getDeveloperId())));

            if (!canDownload) {
                return Result.error("褰撳墠娓告垙涓嶅彲涓嬭浇");
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
            return Result.error("鐢熸垚涓嬭浇閾炬帴澶辫触: " + e.getMessage());
        }
    }

    public Result<List<GameVersion>> getGameVersions(Long gameId, User currentUser) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            return Result.error("娓告垙涓嶅瓨鍦?);
        }
        if (currentUser == null) {
            return Result.error("缂哄皯鏈夋晥鐨勭櫥褰曞嚟璇?);
        }
        boolean canView = currentUser.getRole() == User.UserRole.ADMIN
                || currentUser.getId().equals(game.getDeveloperId());
        if (!canView) {
            return Result.error("鏃犳潈鏌ョ湅璇ユ父鎴忕増鏈?);
        }
        return Result.success(gameVersionRepository.findByGameIdOrderByCreatedAtDesc(gameId));
    }

    public Result<Void> submitGameForAudit(Long id, User currentUser, String requestUri, String note) {
        Game game = gameRepository.findById(id).orElse(null);
        if (game == null) {
            auditLogService.logGameAudit("GAME_SUBMIT", currentUser, null, false, "娓告垙涓嶅瓨鍦?, requestUri);
            return Result.error("娓告垙涓嶅瓨鍦?);
        }

        boolean canSubmit = currentUser.getRole() == User.UserRole.ADMIN
                || currentUser.getId().equals(game.getDeveloperId());
        if (!canSubmit) {
            auditLogService.logGameAudit("GAME_SUBMIT", currentUser, game, false, "鏃犳潈闄愭彁浜?, requestUri);
            return Result.error("鏃犳潈鎻愪氦姝ゆ父鎴?);
        }

        GameVersion latest = gameVersionRepository.findTopByGameIdOrderByCreatedAtDesc(game.getId()).orElse(null);
        if (latest == null) {
            auditLogService.logGameAudit("GAME_SUBMIT", currentUser, game, false, "缂哄皯鍙彁浜ょ増鏈?, requestUri);
            return Result.error("鏈壘鍒板彲鎻愪氦鐨勬父鎴忕増鏈?);
        }
        if (!(latest.getStatus() == GameVersion.VersionStatus.DRAFT
                || latest.getStatus() == GameVersion.VersionStatus.REJECTED)) {
            auditLogService.logGameAudit("GAME_SUBMIT", currentUser, game, false, "闈炴硶鐘舵€佹祦杞?, requestUri);
            return Result.error("褰撳墠鐗堟湰鐘舵€佷笉鍏佽鎻愪氦瀹℃牳");
        }

        latest.setStatus(GameVersion.VersionStatus.SUBMITTED);
        latest.setSubmitNote(note == null ? null : note.trim());
        gameVersionRepository.save(latest);

        game.setStatus(Game.GameStatus.PENDING);
        game.setVersion(latest.getVersionName());
        game.setMd5(latest.getMd5());
        game.setStorageKey(latest.getStorageKey());
        game.setDownloadUrl(buildControlPlaneDownloadUrl(game.getAppId()));
        gameRepository.save(game);
        auditLogService.logGameAudit("GAME_SUBMIT", currentUser, game, true, "鎻愪氦瀹℃牳", requestUri);
        return Result.success();
    }

    public Result<Void> submitGameVersionForAudit(
            Long gameId,
            Long versionId,
            User currentUser,
            String requestUri,
            String note
    ) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, null, false, "娓告垙涓嶅瓨鍦?, requestUri);
            return Result.error("娓告垙涓嶅瓨鍦?);
        }

        boolean canSubmit = currentUser.getRole() == User.UserRole.ADMIN
                || currentUser.getId().equals(game.getDeveloperId());
        if (!canSubmit) {
            auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, game, false, "鏃犳潈闄愭彁浜?, requestUri);
            return Result.error("鏃犳潈鎻愪氦姝ゆ父鎴忕増鏈?);
        }

        GameVersion version = gameVersionRepository.findByIdAndGameId(versionId, gameId).orElse(null);
        if (version == null) {
            auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, game, false, "鐗堟湰涓嶅瓨鍦?, requestUri);
            return Result.error("娓告垙鐗堟湰涓嶅瓨鍦?);
        }
        if (!(version.getStatus() == GameVersion.VersionStatus.DRAFT
                || version.getStatus() == GameVersion.VersionStatus.REJECTED)) {
            auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, game, false, "闈炴硶鐘舵€佹祦杞?, requestUri);
            return Result.error("璇ョ増鏈綋鍓嶇姸鎬佷笉鍏佽鎻愪氦瀹℃牳");
        }

        version.setStatus(GameVersion.VersionStatus.SUBMITTED);
        version.setSubmitNote(note == null ? null : note.trim());
        gameVersionRepository.save(version);

        game.setStatus(Game.GameStatus.PENDING);
        game.setVersion(version.getVersionName());
        game.setMd5(version.getMd5());
        game.setStorageKey(version.getStorageKey());
        game.setDownloadUrl(buildControlPlaneDownloadUrl(game.getAppId()));
        gameRepository.save(game);

        auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, game, true, "鎻愪氦鎸囧畾鐗堟湰瀹℃牳", requestUri);
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
            auditLogService.logGameAudit("GAME_ROLLBACK", currentUser, null, false, "娓告垙涓嶅瓨鍦?, requestUri);
            return Result.error("娓告垙涓嶅瓨鍦?);
        }

        boolean canOperate = currentUser.getRole() == User.UserRole.ADMIN
                || currentUser.getId().equals(game.getDeveloperId());
        if (!canOperate) {
            auditLogService.logGameAudit("GAME_ROLLBACK", currentUser, game, false, "鏃犳潈闄愬洖婊?, requestUri);
            return Result.error("鏃犳潈鍥炴粴姝ゆ父鎴忕増鏈?);
        }

        GameVersion target = gameVersionRepository.findByIdAndGameId(versionId, gameId).orElse(null);
        if (target == null) {
            auditLogService.logGameAudit("GAME_ROLLBACK", currentUser, game, false, "鐗堟湰涓嶅瓨鍦?, requestUri);
            return Result.error("鐩爣鐗堟湰涓嶅瓨鍦?);
        }
        if (target.getStatus() != GameVersion.VersionStatus.APPROVED) {
            auditLogService.logGameAudit("GAME_ROLLBACK", currentUser, game, false, "鐗堟湰鏈鏍搁€氳繃", requestUri);
            return Result.error("浠呮敮鎸佸洖婊氬埌宸插鏍搁€氳繃鐗堟湰");
        }

        game.setStatus(Game.GameStatus.APPROVED);
        game.setVersion(target.getVersionName());
        game.setMd5(target.getMd5());
        game.setStorageKey(target.getStorageKey());
        game.setDownloadUrl(buildControlPlaneDownloadUrl(game.getAppId()));
        gameRepository.save(game);

        String finalReason = (reason == null || reason.isBlank()) ? "鐗堟湰鍥炴粴鍙戝竷" : reason.trim();
        auditLogService.logGameAudit("GAME_ROLLBACK", currentUser, game, true, finalReason, requestUri);
        return Result.success();
    }

    public Result<Void> approveGame(Long id, User currentUser, String requestUri, String reason) {
        if (reason == null || reason.trim().length() < 2) {
            return Result.error("瀹℃牳鍘熷洜鑷冲皯 2 涓瓧绗?);
        }
        Game game = gameRepository.findById(id).orElse(null);
        if (game == null) {
            auditLogService.logGameAudit("GAME_APPROVE", currentUser, null, false, "娓告垙涓嶅瓨鍦?, requestUri);
            return Result.error("娓告垙涓嶅瓨鍦?);
        }
        GameVersion submitted = gameVersionRepository
                .findTopByGameIdAndStatusOrderByCreatedAtDesc(game.getId(), GameVersion.VersionStatus.SUBMITTED)
                .orElse(null);
        if (submitted == null || game.getStatus() != Game.GameStatus.PENDING) {
            auditLogService.logGameAudit("GAME_APPROVE", currentUser, game, false, "闈炴硶鐘舵€佹祦杞?, requestUri);
            return Result.error("褰撳墠鐘舵€佷笉鍏佽瀹℃牳閫氳繃");
        }

        submitted.setStatus(GameVersion.VersionStatus.APPROVED);
        submitted.setAuditReason(reason.trim());
        gameVersionRepository.save(submitted);

        game.setStatus(Game.GameStatus.APPROVED);
        game.setVersion(submitted.getVersionName());
        game.setMd5(submitted.getMd5());
        game.setStorageKey(submitted.getStorageKey());
        game.setDownloadUrl(buildControlPlaneDownloadUrl(game.getAppId()));
        gameRepository.save(game);
        auditLogService.logGameAudit("GAME_APPROVE", currentUser, game, true, reason.trim(), requestUri);
        return Result.success();
    }

    public Result<Void> rejectGame(Long id, User currentUser, String requestUri, String reason) {
        if (reason == null || reason.trim().length() < 2) {
            return Result.error("椹冲洖鍘熷洜鑷冲皯 2 涓瓧绗?);
        }
        Game game = gameRepository.findById(id).orElse(null);
        if (game == null) {
            auditLogService.logGameAudit("GAME_REJECT", currentUser, null, false, "娓告垙涓嶅瓨鍦?, requestUri);
            return Result.error("娓告垙涓嶅瓨鍦?);
        }
        GameVersion submitted = gameVersionRepository
                .findTopByGameIdAndStatusOrderByCreatedAtDesc(game.getId(), GameVersion.VersionStatus.SUBMITTED)
                .orElse(null);
        if (submitted == null || game.getStatus() != Game.GameStatus.PENDING) {
            auditLogService.logGameAudit("GAME_REJECT", currentUser, game, false, "闈炴硶鐘舵€佹祦杞?, requestUri);
            return Result.error("褰撳墠鐘舵€佷笉鍏佽瀹℃牳鎷掔粷");
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
            return "涓婁紶鏂囦欢涓嶈兘涓虹┖";
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            return "浠呮敮鎸?ZIP 鏍煎紡涓婁紶";
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
                    return "鍘嬬缉鍖呮枃浠舵暟閲忚繃澶?;
                }
                String name = entry.getName();
                if (name == null || name.isBlank() || name.contains("..") || name.startsWith("/") || name.startsWith("\\")) {
                    return "鍘嬬缉鍖呭寘鍚潪娉曡矾寰?;
                }
                if ("index.html".equalsIgnoreCase(name)) {
                    hasIndexHtml = true;
                }
                int read;
                while ((read = zis.read(buffer)) != -1) {
                    totalSize += read;
                    if (totalSize > 200L * 1024 * 1024) {
                        return "鍘嬬缉鍖呰В鍘嬪悗浣撶Н瓒呰繃闄愬埗";
                    }
                }
            }

            if (!hasIndexHtml) {
                return "鍘嬬缉鍖呯己灏?index.html";
            }
        } catch (Exception e) {
            return "鍘嬬缉鍖呮牎楠屽け璐? " + e.getMessage();
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

    private void normalizeClientUrls(Game game) {
        if (game == null || game.getAppId() == null) {
            return;
        }
        String downloadUrl = game.getDownloadUrl();
        if (downloadUrl == null || downloadUrl.isBlank()
                || downloadUrl.contains("/game/download-url/")
                || downloadUrl.endsWith("/game/download/" + game.getAppId())) {
            game.setDownloadUrl(buildControlPlaneDownloadUrl(game.getAppId()));
        }
    }
}






