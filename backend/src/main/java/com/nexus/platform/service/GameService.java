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
            return Result.error("上传失败: " + e.getMessage());
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
            return Result.error("游戏不存在");
        }
        normalizeClientUrls(game);
        return Result.success(game);
    }

    public Result<List<Game>> getDeveloperGames(Long developerId, User currentUser) {
        if (currentUser == null) {
            return Result.error("缺少有效的登录凭证");
        }
        if (currentUser.getRole() != User.UserRole.ADMIN && !currentUser.getId().equals(developerId)) {
            return Result.error("无权查看其他开发者的游戏列表");
        }
        List<Game> games = gameRepository.findByDeveloperIdOrderByCreatedAtDesc(developerId);
        games.forEach(this::normalizeClientUrls);
        return Result.success(games);
    }

    public Result<PageResult<Game>> getDeveloperGamesPaged(Long developerId, User currentUser, int page, int size) {
        if (currentUser == null) {
            return Result.error("缺少有效的登录凭证");
        }
        if (currentUser.getRole() != User.UserRole.ADMIN && !currentUser.getId().equals(developerId)) {
            return Result.error("无权查看其他开发者的游戏列表");
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
                return Result.error("游戏不存在");
            }

            boolean canDownload = game.getStatus() == Game.GameStatus.APPROVED
                    || (currentUser != null && (currentUser.getRole() == User.UserRole.ADMIN
                    || currentUser.getId().equals(game.getDeveloperId())));

            if (!canDownload) {
                return Result.error("当前游戏不可下载");
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
            return Result.error("生成下载链接失败: " + e.getMessage());
        }
    }

    public Result<List<GameVersion>> getGameVersions(Long gameId, User currentUser) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            return Result.error("游戏不存在");
        }
        if (currentUser == null) {
            return Result.error("缺少有效的登录凭证");
        }
        boolean canView = currentUser.getRole() == User.UserRole.ADMIN
                || currentUser.getId().equals(game.getDeveloperId());
        if (!canView) {
            return Result.error("无权查看该游戏版本");
        }
        return Result.success(gameVersionRepository.findByGameIdOrderByCreatedAtDesc(gameId));
    }

    public Result<Void> submitGameForAudit(Long id, User currentUser, String requestUri, String note) {
        Game game = gameRepository.findById(id).orElse(null);
        if (game == null) {
            auditLogService.logGameAudit("GAME_SUBMIT", currentUser, null, false, "游戏不存在", requestUri);
            return Result.error("游戏不存在");
        }

        boolean canSubmit = currentUser.getRole() == User.UserRole.ADMIN
                || currentUser.getId().equals(game.getDeveloperId());
        if (!canSubmit) {
            auditLogService.logGameAudit("GAME_SUBMIT", currentUser, game, false, "无权限提交", requestUri);
            return Result.error("无权提交此游戏");
        }

        GameVersion latest = gameVersionRepository.findTopByGameIdOrderByCreatedAtDesc(game.getId()).orElse(null);
        if (latest == null) {
            auditLogService.logGameAudit("GAME_SUBMIT", currentUser, game, false, "缺少可提交版本", requestUri);
            return Result.error("未找到可提交的游戏版本");
        }
        if (!(latest.getStatus() == GameVersion.VersionStatus.DRAFT
                || latest.getStatus() == GameVersion.VersionStatus.REJECTED)) {
            auditLogService.logGameAudit("GAME_SUBMIT", currentUser, game, false, "非法状态流转", requestUri);
            return Result.error("当前版本状态不允许提交审核");
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
        auditLogService.logGameAudit("GAME_SUBMIT", currentUser, game, true, "提交审核", requestUri);
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
            auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, null, false, "游戏不存在", requestUri);
            return Result.error("游戏不存在");
        }

        boolean canSubmit = currentUser.getRole() == User.UserRole.ADMIN
                || currentUser.getId().equals(game.getDeveloperId());
        if (!canSubmit) {
            auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, game, false, "无权限提交", requestUri);
            return Result.error("无权提交此游戏版本");
        }

        GameVersion version = gameVersionRepository.findByIdAndGameId(versionId, gameId).orElse(null);
        if (version == null) {
            auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, game, false, "版本不存在", requestUri);
            return Result.error("游戏版本不存在");
        }
        if (!(version.getStatus() == GameVersion.VersionStatus.DRAFT
                || version.getStatus() == GameVersion.VersionStatus.REJECTED)) {
            auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, game, false, "非法状态流转", requestUri);
            return Result.error("该版本当前状态不允许提交审核");
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

        auditLogService.logGameAudit("GAME_SUBMIT_VERSION", currentUser, game, true, "提交指定版本审核", requestUri);
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
            auditLogService.logGameAudit("GAME_ROLLBACK", currentUser, null, false, "游戏不存在", requestUri);
            return Result.error("游戏不存在");
        }

        boolean canOperate = currentUser.getRole() == User.UserRole.ADMIN
                || currentUser.getId().equals(game.getDeveloperId());
        if (!canOperate) {
            auditLogService.logGameAudit("GAME_ROLLBACK", currentUser, game, false, "无权限回滚", requestUri);
            return Result.error("无权回滚此游戏版本");
        }

        GameVersion target = gameVersionRepository.findByIdAndGameId(versionId, gameId).orElse(null);
        if (target == null) {
            auditLogService.logGameAudit("GAME_ROLLBACK", currentUser, game, false, "版本不存在", requestUri);
            return Result.error("目标版本不存在");
        }
        if (target.getStatus() != GameVersion.VersionStatus.APPROVED) {
            auditLogService.logGameAudit("GAME_ROLLBACK", currentUser, game, false, "版本未审核通过", requestUri);
            return Result.error("仅支持回滚到已审核通过版本");
        }

        game.setStatus(Game.GameStatus.APPROVED);
        game.setVersion(target.getVersionName());
        game.setMd5(target.getMd5());
        game.setStorageKey(target.getStorageKey());
        game.setDownloadUrl(buildControlPlaneDownloadUrl(game.getAppId()));
        gameRepository.save(game);

        String finalReason = (reason == null || reason.isBlank()) ? "版本回滚发布" : reason.trim();
        auditLogService.logGameAudit("GAME_ROLLBACK", currentUser, game, true, finalReason, requestUri);
        return Result.success();
    }

    public Result<Void> approveGame(Long id, User currentUser, String requestUri, String reason) {
        if (reason == null || reason.trim().length() < 2) {
            return Result.error("审核原因至少 2 个字符");
        }
        Game game = gameRepository.findById(id).orElse(null);
        if (game == null) {
            auditLogService.logGameAudit("GAME_APPROVE", currentUser, null, false, "游戏不存在", requestUri);
            return Result.error("游戏不存在");
        }
        GameVersion submitted = gameVersionRepository
                .findTopByGameIdAndStatusOrderByCreatedAtDesc(game.getId(), GameVersion.VersionStatus.SUBMITTED)
                .orElse(null);
        if (submitted == null || game.getStatus() != Game.GameStatus.PENDING) {
            auditLogService.logGameAudit("GAME_APPROVE", currentUser, game, false, "非法状态流转", requestUri);
            return Result.error("当前状态不允许审核通过");
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
            return Result.error("驳回原因至少 2 个字符");
        }
        Game game = gameRepository.findById(id).orElse(null);
        if (game == null) {
            auditLogService.logGameAudit("GAME_REJECT", currentUser, null, false, "游戏不存在", requestUri);
            return Result.error("游戏不存在");
        }
        GameVersion submitted = gameVersionRepository
                .findTopByGameIdAndStatusOrderByCreatedAtDesc(game.getId(), GameVersion.VersionStatus.SUBMITTED)
                .orElse(null);
        if (submitted == null || game.getStatus() != Game.GameStatus.PENDING) {
            auditLogService.logGameAudit("GAME_REJECT", currentUser, game, false, "非法状态流转", requestUri);
            return Result.error("当前状态不允许审核拒绝");
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
            return "上传文件不能为空";
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            return "仅支持 ZIP 格式上传";
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
                    return "压缩包文件数量过多";
                }
                String name = entry.getName();
                if (name == null || name.isBlank() || name.contains("..") || name.startsWith("/") || name.startsWith("\\")) {
                    return "压缩包包含非法路径";
                }
                if ("index.html".equalsIgnoreCase(name)) {
                    hasIndexHtml = true;
                }
                int read;
                while ((read = zis.read(buffer)) != -1) {
                    totalSize += read;
                    if (totalSize > 200L * 1024 * 1024) {
                        return "压缩包解压后体积超过限制";
                    }
                }
            }

            if (!hasIndexHtml) {
                return "压缩包缺少 index.html";
            }
        } catch (Exception e) {
            return "压缩包校验失败: " + e.getMessage();
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






