package com.nexus.platform.service;

import com.nexus.platform.dto.GameMetadataUpdateRequest;
import com.nexus.platform.dto.OpsAdminReviewerOptionDto;
import com.nexus.platform.dto.OpsBatchActionResultDto;
import com.nexus.platform.dto.OpsGameGovernanceItemDto;
import com.nexus.platform.dto.OpsGameGovernanceImpactDto;
import com.nexus.platform.dto.OpsGameGovernanceScopeDto;
import com.nexus.platform.dto.OpsGameVersionDirectPublishRequest;
import com.nexus.platform.dto.OpsGameGovernanceScopeUpdateRequest;
import com.nexus.platform.dto.OpsGameVisibilityBatchUpdateRequest;
import com.nexus.platform.dto.OpsGameVisibilityUpdateRequest;
import com.nexus.platform.dto.OpsReviewOverviewDto;
import com.nexus.platform.dto.OpsReviewAssignmentRequest;
import com.nexus.platform.dto.OpsReviewBatchActionRequest;
import com.nexus.platform.dto.OpsReviewItemDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.GameVersion;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.GameVersionRepository;
import com.nexus.platform.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class OpsGameGovernanceService {
    private static final Set<String> REVIEWABLE_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED");
    private static final Set<String> CHANNEL_REGION_MODES = Set.of("ALL", "ALLOWLIST", "DENYLIST");
    private static final Set<String> VERSION_MODES = Set.of("ALL", "MIN_VERSION", "RANGE", "BLOCKLIST");

    private final GameRepository gameRepository;
    private final GameVersionRepository gameVersionRepository;
    private final UserRepository userRepository;
    private final GameService gameService;

    public Result<List<OpsGameGovernanceItemDto>> listGames(
            String status,
            String category,
            Long developerId,
            String keyword
    ) {
        String normalizedStatus = normalizeStatus(status);
        if (status != null && normalizedStatus == null) {
            return Result.error("Invalid game status");
        }
        if (developerId != null && developerId <= 0) {
            return Result.error("Invalid developer id");
        }
        String normalizedKeyword = normalizeKeyword(keyword);
        if (keyword != null && normalizedKeyword == null) {
            return Result.error("Invalid keyword");
        }
        String normalizedCategory = normalizeCategory(category);
        if (category != null && normalizedCategory == null) {
            return Result.error("Invalid category");
        }

        List<Game> games = gameRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, List<GameVersion>> versionsByGameId = gameVersionRepository.findAll().stream()
                .collect(Collectors.groupingBy(GameVersion::getGameId));

        List<OpsGameGovernanceItemDto> rows = games.stream()
                .filter(game -> matchesGameFilters(game, normalizedStatus, normalizedCategory, developerId, normalizedKeyword))
                .map(game -> toGovernanceItem(game, versionsByGameId.getOrDefault(game.getId(), List.of())))
                .toList();

        return Result.success(rows);
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
        return gameService.uploadGameAsAdmin(file, name, description, category, tags, requiresOnline, currentUser, requestUri);
    }

    public Result<GameVersion> uploadGameVersionAsAdmin(
            Long gameId,
            MultipartFile file,
            String versionName,
            User currentUser,
            String requestUri
    ) {
        return gameService.uploadGameVersionAsAdmin(gameId, file, versionName, currentUser, requestUri);
    }

    public Result<Void> directPublishVersionAsAdmin(
            Long gameId,
            Long versionId,
            OpsGameVersionDirectPublishRequest request,
            User currentUser,
            String requestUri
    ) {
        return gameService.directPublishVersionAsAdmin(
                gameId,
                versionId,
                currentUser,
                requestUri,
                request == null ? null : request.reason(),
                request == null ? null : request.forceUpdate()
        );
    }

    public Result<List<OpsReviewItemDto>> listReviews(String status, String keyword, Long developerId) {
        String normalizedStatus = normalizeStatus(status);
        if (status != null && normalizedStatus == null) {
            return Result.error("Invalid review status");
        }
        if (normalizedStatus != null && !REVIEWABLE_STATUSES.contains(normalizedStatus)) {
            return Result.error("Review status must be one of PENDING, APPROVED, REJECTED");
        }
        if (developerId != null && developerId <= 0) {
            return Result.error("Invalid developer id");
        }
        String normalizedKeyword = normalizeKeyword(keyword);
        if (keyword != null && normalizedKeyword == null) {
            return Result.error("Invalid keyword");
        }

        List<Game> games = gameRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, List<GameVersion>> versionsByGameId = gameVersionRepository.findAll().stream()
                .collect(Collectors.groupingBy(GameVersion::getGameId));

        List<OpsReviewItemDto> rows = games.stream()
                .map(game -> {
                    GameVersion reviewVersion = selectReviewVersion(game, versionsByGameId.getOrDefault(game.getId(), List.of()));
                    if (reviewVersion == null) {
                        return null;
                    }
                    gameService.attachHostedManifestReport(reviewVersion);
                    return toReviewItem(game, reviewVersion);
                })
                .filter(item -> item != null)
                .filter(item -> matchesReviewFilters(item, normalizedStatus, developerId, normalizedKeyword))
                .sorted(Comparator.comparing(OpsReviewItemDto::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return Result.success(rows);
    }

    public Result<OpsReviewOverviewDto> getReviewOverview() {
        List<OpsReviewItemDto> rows = listReviews(null, null, null).getData();
        if (rows == null) {
            rows = List.of();
        }
        List<OpsReviewItemDto> pendingRows = rows.stream().filter(item -> "PENDING".equals(item.gameStatus())).toList();
        List<User> reviewers = userRepository.findByRoleAndAccountStatusOrderByCreatedAtDesc(User.UserRole.ADMIN, "ACTIVE");

        int pending = pendingRows.size();
        int assigned = (int) pendingRows.stream().filter(item -> item.assignedReviewerId() != null).count();
        int unassigned = pending - assigned;
        int overdue = (int) pendingRows.stream().filter(item -> Boolean.TRUE.equals(item.overdue())).count();
        int dueSoon = (int) pendingRows.stream().filter(item -> isDueSoon(item.dueAt())).count();
        int approved = (int) rows.stream().filter(item -> "APPROVED".equals(item.gameStatus())).count();
        int rejected = (int) rows.stream().filter(item -> "REJECTED".equals(item.gameStatus())).count();
        double avgPendingHours = pendingRows.isEmpty()
                ? 0D
                : pendingRows.stream().mapToLong(item -> item.pendingHours() == null ? 0L : item.pendingHours()).average().orElse(0D);

        List<OpsReviewOverviewDto.ReviewerLoad> reviewerLoads = reviewers.stream()
                .map(reviewer -> {
                    int assignedPending = (int) pendingRows.stream().filter(item -> reviewer.getId().equals(item.assignedReviewerId())).count();
                    int overduePending = (int) pendingRows.stream().filter(item -> reviewer.getId().equals(item.assignedReviewerId()) && Boolean.TRUE.equals(item.overdue())).count();
                    return new OpsReviewOverviewDto.ReviewerLoad(reviewer.getId(), reviewer.getUsername(), assignedPending, overduePending);
                })
                .filter(item -> item.assignedPending() > 0 || item.overduePending() > 0)
                .sorted(Comparator.comparing(OpsReviewOverviewDto.ReviewerLoad::overduePending).reversed()
                        .thenComparing(OpsReviewOverviewDto.ReviewerLoad::assignedPending).reversed())
                .toList();

        List<OpsReviewOverviewDto.BacklogBucket> backlogBuckets = List.of(
                new OpsReviewOverviewDto.BacklogBucket("NORMAL", "0-24H", (int) pendingRows.stream().filter(item -> "NORMAL".equals(item.backlogLevel())).count()),
                new OpsReviewOverviewDto.BacklogBucket("RISK", "24-72H", (int) pendingRows.stream().filter(item -> "RISK".equals(item.backlogLevel())).count()),
                new OpsReviewOverviewDto.BacklogBucket("OVERDUE", "72H+", (int) pendingRows.stream().filter(item -> "OVERDUE".equals(item.backlogLevel())).count())
        );

        return Result.success(new OpsReviewOverviewDto(
                pending,
                assigned,
                unassigned,
                overdue,
                dueSoon,
                approved,
                rejected,
                0,
                Math.round(avgPendingHours * 10D) / 10D,
                reviewerLoads,
                backlogBuckets
        ));
    }

    public Result<List<OpsAdminReviewerOptionDto>> listReviewers() {
        return Result.success(userRepository.findByRoleAndAccountStatusOrderByCreatedAtDesc(User.UserRole.ADMIN, "ACTIVE").stream()
                .map(item -> new OpsAdminReviewerOptionDto(item.getId(), item.getUsername()))
                .toList());
    }

    public Result<Game> updateGameMetadataAsAdmin(
            Long gameId,
            GameMetadataUpdateRequest request,
            User currentUser,
            String requestUri
    ) {
        return gameService.updateGameMetadataInternal(gameId, request, currentUser, requestUri, true);
    }

    public Result<Game> updateGameVisibilityAsAdmin(
            Long gameId,
            OpsGameVisibilityUpdateRequest request,
            User currentUser,
            String requestUri
    ) {
        return gameService.updateGameVisibility(gameId, request, currentUser, requestUri);
    }

    public Result<OpsGameGovernanceScopeDto> updateGameGovernanceScopeAsAdmin(
            Long gameId,
            OpsGameGovernanceScopeUpdateRequest request,
            User currentUser,
            String requestUri
    ) {
        if (gameId == null || gameId <= 0) {
            return Result.error("Invalid game id");
        }
        if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
            return Result.error("No permission to update governance scope");
        }
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            return Result.error("Game not found");
        }
        if (request == null) {
            return Result.error("Request body is required");
        }

        String channelMode = normalizeChannelRegionMode(request.channelMode());
        String regionMode = normalizeChannelRegionMode(request.regionMode());
        String versionMode = normalizeVersionMode(request.versionMode());
        if (channelMode == null || regionMode == null || versionMode == null) {
            return Result.error("Invalid governance mode");
        }

        List<String> channels = normalizeDimensionValues(request.channels(), 20, 64);
        List<String> regions = normalizeDimensionValues(request.regions(), 50, 64);
        List<String> blockedVersions = normalizeDimensionValues(request.blockedVersions(), 20, 64);
        if (channels == null || regions == null || blockedVersions == null) {
            return Result.error("Governance values are invalid");
        }
        if (!"ALL".equals(channelMode) && channels.isEmpty()) {
            return Result.error("Channels are required for the selected channel mode");
        }
        if (!"ALL".equals(regionMode) && regions.isEmpty()) {
            return Result.error("Regions are required for the selected region mode");
        }

        String versionMin = normalizeVersionToken(request.versionMin());
        String versionMax = normalizeVersionToken(request.versionMax());
        if ("MIN_VERSION".equals(versionMode) && versionMin == null) {
            return Result.error("Minimum version is required");
        }
        if ("RANGE".equals(versionMode) && versionMin == null && versionMax == null) {
            return Result.error("At least one version bound is required");
        }
        if ("BLOCKLIST".equals(versionMode) && blockedVersions.isEmpty()) {
            return Result.error("Blocked versions are required");
        }
        if ("ALL".equals(versionMode) && (!blockedVersions.isEmpty() || versionMin != null || versionMax != null)) {
            return Result.error("Version constraints must be empty when version mode is ALL");
        }
        if (versionMin != null && versionMax != null && compareVersionTokens(versionMin, versionMax) > 0) {
            return Result.error("Minimum version cannot be greater than maximum version");
        }

        String governanceNote = request.governanceNote() == null ? null : request.governanceNote().trim();
        if (governanceNote != null && governanceNote.length() > 256) {
            return Result.error("Governance note is too long");
        }

        game.setChannelGovernanceMode(channelMode);
        game.setChannelGovernanceValues(joinValues("ALL".equals(channelMode) ? List.of() : channels));
        game.setRegionGovernanceMode(regionMode);
        game.setRegionGovernanceValues(joinValues("ALL".equals(regionMode) ? List.of() : regions));
        game.setVersionGovernanceMode(versionMode);
        game.setVersionMin("ALL".equals(versionMode) || "BLOCKLIST".equals(versionMode) ? null : versionMin);
        game.setVersionMax("ALL".equals(versionMode) || "MIN_VERSION".equals(versionMode) || "BLOCKLIST".equals(versionMode) ? null : versionMax);
        game.setVersionBlocklist("BLOCKLIST".equals(versionMode) ? joinValues(blockedVersions) : null);
        game.setGovernanceNote(governanceNote == null || governanceNote.isBlank() ? null : governanceNote);
        gameRepository.save(game);

        OpsGameGovernanceImpactDto impact = buildGovernanceImpact(game);
        gameService.recordReviewAudit(
                "GAME_SCOPE_GOVERNANCE_UPDATE",
                currentUser,
                game,
                null,
                impact.summary(),
                requestUri
        );
        return Result.success(toGovernanceScope(game));
    }

    public Result<OpsGameGovernanceImpactDto> getGovernanceImpact(Long gameId) {
        if (gameId == null || gameId <= 0) {
            return Result.error("Invalid game id");
        }
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            return Result.error("Game not found");
        }
        return Result.success(buildGovernanceImpact(game));
    }

    public Result<OpsReviewItemDto> assignReviewTask(
            Long versionId,
            OpsReviewAssignmentRequest request,
            User currentUser,
            String requestUri
    ) {
        if (versionId == null || versionId <= 0) {
            return Result.error("Invalid version id");
        }
        if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
            return Result.error("No permission to assign review");
        }
        GameVersion version = gameVersionRepository.findById(versionId).orElse(null);
        if (version == null) {
            return Result.error("Review version not found");
        }
        if (version.getStatus() != GameVersion.VersionStatus.SUBMITTED) {
            return Result.error("Only submitted versions can be assigned");
        }
        Long reviewerId = request == null ? null : request.reviewerId();
        if (reviewerId == null || reviewerId <= 0) {
            return Result.error("Reviewer id is required");
        }
        User reviewer = userRepository.findById(reviewerId).orElse(null);
        if (reviewer == null || reviewer.getRole() != User.UserRole.ADMIN) {
            return Result.error("Reviewer not found");
        }
        version.setAssignedReviewerId(reviewerId);
        version.setAssignedAt(LocalDateTime.now());
        gameVersionRepository.save(version);
        Game game = gameRepository.findById(version.getGameId()).orElse(null);
        if (game == null) {
            return Result.error("Game not found");
        }
        gameService.attachHostedManifestReport(version);
        gameService.recordReviewAudit("REVIEW_REASSIGNED", currentUser, game, version, request == null ? null : request.note(), requestUri);
        return Result.success(toReviewItem(game, version));
    }

    public Result<OpsBatchActionResultDto> updateGameVisibilityBatchAsAdmin(
            OpsGameVisibilityBatchUpdateRequest request,
            User currentUser,
            String requestUri
    ) {
        if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
            return Result.error("No permission to batch update game visibility");
        }
        List<Long> gameIds = request == null || request.gameIds() == null ? List.of() : request.gameIds().stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (gameIds.isEmpty() || gameIds.size() > 100) {
            return Result.error("Batch size must be 1-100");
        }
        String status = normalizeVisibilityStatus(request.visibilityStatus());
        if (status == null) {
            return Result.error("Invalid visibility status");
        }
        String reason = request.reason() == null ? null : request.reason().trim();
        LocalDateTime until = request.visibilityUntil();
        if (!"VISIBLE".equals(status)) {
            if (reason == null || reason.length() < 2) {
                return Result.error("Reason must be at least 2 chars");
            }
            if (reason.length() > 256) {
                return Result.error("Reason is too long");
            }
        }
        if ("BLOCKED".equals(status) && until != null && until.isBefore(LocalDateTime.now())) {
            return Result.error("Visibility until cannot be in the past");
        }

        List<Game> games = gameRepository.findAllById(gameIds);
        if (games.size() != gameIds.size()) {
            return Result.error("Some games were not found");
        }
        List<Long> affected = new ArrayList<>();
        for (Game game : games) {
            game.setVisibilityStatus(status);
            game.setVisibilityReason("VISIBLE".equals(status) ? null : reason);
            game.setVisibilityUntil("BLOCKED".equals(status) ? until : null);
            affected.add(game.getId());
        }
        gameRepository.saveAll(games);
        for (Game game : games) {
            gameService.recordVisibilityAudit(
                    game,
                    currentUser,
                    "Set visibility to " + status + (reason == null ? "" : " (" + reason + ")"),
                    requestUri
            );
        }
        return Result.success(new OpsBatchActionResultDto(gameIds.size(), affected.size(), "GAME_VISIBILITY_BATCH_UPDATE", affected));
    }

    public Result<OpsBatchActionResultDto> batchReviewAction(
            OpsReviewBatchActionRequest request,
            User currentUser,
            String requestUri
    ) {
        if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
            return Result.error("No permission to batch update reviews");
        }
        List<Long> versionIds = request == null || request.versionIds() == null
                ? List.of()
                : request.versionIds().stream().filter(id -> id != null && id > 0).distinct().toList();
        if (versionIds.isEmpty() || versionIds.size() > 100) {
            return Result.error("Batch size must be 1-100");
        }
        String action = normalizeBatchReviewAction(request.action());
        if (action == null) {
            return Result.error("Invalid batch review action");
        }

        List<GameVersion> versions = gameVersionRepository.findAllById(versionIds);
        if (versions.size() != versionIds.size()) {
            return Result.error("Some review versions were not found");
        }
        Map<Long, Game> gamesById = gameRepository.findAllById(
                versions.stream().map(GameVersion::getGameId).distinct().toList()
        ).stream().collect(Collectors.toMap(Game::getId, game -> game));
        for (GameVersion version : versions) {
            if (version.getStatus() != GameVersion.VersionStatus.SUBMITTED) {
                return Result.error("Only submitted versions can be batch processed");
            }
            if (!gamesById.containsKey(version.getGameId())) {
                return Result.error("Game not found");
            }
        }

        if ("ASSIGN".equals(action)) {
            Long reviewerId = request.reviewerId();
            if (reviewerId == null || reviewerId <= 0) {
                return Result.error("Reviewer id is required");
            }
            User reviewer = userRepository.findById(reviewerId).orElse(null);
            if (reviewer == null || reviewer.getRole() != User.UserRole.ADMIN) {
                return Result.error("Reviewer not found");
            }
            LocalDateTime now = LocalDateTime.now();
            for (GameVersion version : versions) {
                version.setAssignedReviewerId(reviewerId);
                version.setAssignedAt(now);
            }
            gameVersionRepository.saveAll(versions);
            for (GameVersion version : versions) {
                gameService.recordReviewAudit("REVIEW_REASSIGNED", currentUser, gamesById.get(version.getGameId()), version, request.note(), requestUri);
            }
        } else if ("APPROVE".equals(action)) {
            String reason = request.reason() == null ? null : request.reason().trim();
            if (reason == null || reason.length() < 2) {
                return Result.error("Audit reason must be at least 2 chars");
            }
            for (GameVersion version : versions) {
                Result<Void> result = gameService.approveGame(version.getGameId(), currentUser, requestUri, reason);
                if (result.getCode() != 0) {
                    return Result.error(result.getMessage());
                }
            }
        } else if ("REJECT".equals(action)) {
            String reason = request.reason() == null ? null : request.reason().trim();
            if (reason == null || reason.length() < 2) {
                return Result.error("Reject reason must be at least 2 chars");
            }
            for (GameVersion version : versions) {
                Result<Void> result = gameService.rejectGame(version.getGameId(), currentUser, requestUri, reason);
                if (result.getCode() != 0) {
                    return Result.error(result.getMessage());
                }
            }
        }

        return Result.success(new OpsBatchActionResultDto(versionIds.size(), versionIds.size(), "REVIEW_BATCH_" + action, versionIds));
    }

    private OpsGameGovernanceItemDto toGovernanceItem(Game game, List<GameVersion> versions) {
        GameVersion latest = versions.stream()
                .max(Comparator.comparing(GameVersion::getCreatedAt))
                .orElse(null);
        if (latest != null) {
            gameService.attachHostedManifestReport(latest);
        }
        OpsGameGovernanceImpactDto impact = buildGovernanceImpact(game);
        return new OpsGameGovernanceItemDto(
                game.getId(),
                game.getAppId(),
                game.getDeveloperId(),
                game.getName(),
                game.getDescription(),
                game.getIconUrl(),
                game.getCategory(),
                parseTags(game.getTagsJson()),
                game.getVersion(),
                game.getStatus() == null ? null : game.getStatus().name(),
                gameService.deriveFrontendState(game),
                gameService.effectiveVisibilityStatus(game),
                game.getVisibilityReason(),
                game.getVisibilityUntil(),
                toGovernanceScope(game),
                impact.exposureState(),
                impact.impactLevel(),
                impact.activeControlCount(),
                impact.affectedSurfaceCount(),
                impact.summary(),
                Boolean.TRUE.equals(game.getRequiresOnline()),
                versions.size(),
                latest == null || latest.getStatus() == null ? null : latest.getStatus().name(),
                latest == null ? null : latest.getAuditReason(),
                latest == null ? null : latest.getHostedManifestValid(),
                latest == null ? null : latest.getHostedManifestSummary(),
                game.getCreatedAt(),
                game.getUpdatedAt()
        );
    }

    private OpsReviewItemDto toReviewItem(Game game, GameVersion latest) {
        LocalDateTime dueAt = latest.getUpdatedAt() == null ? null : latest.getUpdatedAt().plusDays(3);
        boolean overdue = dueAt != null && LocalDateTime.now().isAfter(dueAt) && latest.getStatus() == GameVersion.VersionStatus.SUBMITTED;
        long pendingHours = latest.getUpdatedAt() == null ? 0L : Math.max(0L, ChronoUnit.HOURS.between(latest.getUpdatedAt(), LocalDateTime.now()));
        return new OpsReviewItemDto(
                latest.getId(),
                game.getId(),
                game.getAppId(),
                game.getName(),
                game.getDeveloperId(),
                game.getCategory(),
                game.getStatus() == null ? null : game.getStatus().name(),
                gameService.effectiveVisibilityStatus(game),
                game.getVisibilityReason(),
                game.getVisibilityUntil(),
                latest.getId(),
                latest.getVersionName(),
                latest.getStatus() == null ? null : latest.getStatus().name(),
                latest.getSubmitNote(),
                latest.getAuditReason(),
                latest.getAssignedReviewerId(),
                latest.getAssignedAt(),
                dueAt,
                overdue,
                pendingHours,
                deriveQueuePriority(latest, overdue, pendingHours),
                deriveBacklogLevel(overdue, pendingHours),
                latest.getForcedUpdate(),
                latest.getHostedManifestValid(),
                latest.getHostedManifestSummary(),
                latest.getCreatedAt(),
                latest.getUpdatedAt(),
                gameService.deriveFrontendState(game)
        );
    }

    private boolean isDueSoon(LocalDateTime dueAt) {
        LocalDateTime now = LocalDateTime.now();
        return dueAt != null && dueAt.isAfter(now) && dueAt.isBefore(now.plusHours(24));
    }

    private String deriveQueuePriority(GameVersion version, boolean overdue, long pendingHours) {
        if (overdue) {
            return "P0";
        }
        if (Boolean.TRUE.equals(version.getForcedUpdate()) || version.getAssignedReviewerId() == null || pendingHours >= 48) {
            return "P1";
        }
        return "P2";
    }

    private String deriveBacklogLevel(boolean overdue, long pendingHours) {
        if (overdue || pendingHours >= 72) {
            return "OVERDUE";
        }
        if (pendingHours >= 24) {
            return "RISK";
        }
        return "NORMAL";
    }

    private GameVersion selectReviewVersion(Game game, List<GameVersion> versions) {
        if (game == null || game.getStatus() == null || versions == null || versions.isEmpty()) {
            return null;
        }
        Comparator<GameVersion> updatedAtDesc = Comparator.comparing(
                GameVersion::getUpdatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())
        ).reversed();

        return switch (game.getStatus()) {
            case PENDING -> versions.stream()
                    .filter(version -> version.getStatus() == GameVersion.VersionStatus.SUBMITTED)
                    .max(updatedAtDesc)
                    .orElse(null);
            case APPROVED -> versions.stream()
                    .filter(version -> version.getStatus() == GameVersion.VersionStatus.APPROVED)
                    .max(updatedAtDesc)
                    .orElse(null);
            case REJECTED -> versions.stream()
                    .filter(version -> version.getStatus() == GameVersion.VersionStatus.REJECTED)
                    .max(updatedAtDesc)
                    .orElse(null);
            default -> null;
        };
    }

    private boolean matchesGameFilters(
            Game game,
            String status,
            String category,
            Long developerId,
            String keyword
    ) {
        if (status != null && (game.getStatus() == null || !status.equals(game.getStatus().name()))) {
            return false;
        }
        if (category != null && (game.getCategory() == null || !category.equalsIgnoreCase(game.getCategory()))) {
            return false;
        }
        if (developerId != null && !developerId.equals(game.getDeveloperId())) {
            return false;
        }
        if (keyword == null) {
            return true;
        }
        String haystack = String.join(" ",
                safe(game.getName()),
                safe(game.getAppId()),
                safe(game.getCategory()),
                safe(game.getDescription()));
        return haystack.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean matchesReviewFilters(
            OpsReviewItemDto item,
            String status,
            Long developerId,
            String keyword
    ) {
        if (status != null && !status.equals(item.gameStatus())) {
            return false;
        }
        if (developerId != null && !developerId.equals(item.developerId())) {
            return false;
        }
        if (keyword == null) {
            return true;
        }
        String haystack = String.join(" ",
                safe(item.name()),
                safe(item.appId()),
                safe(item.category()),
                safe(item.version()));
        return haystack.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private OpsGameGovernanceScopeDto toGovernanceScope(Game game) {
        return new OpsGameGovernanceScopeDto(
                safeMode(game.getChannelGovernanceMode(), "ALL"),
                splitValues(game.getChannelGovernanceValues()),
                safeMode(game.getRegionGovernanceMode(), "ALL"),
                splitValues(game.getRegionGovernanceValues()),
                safeMode(game.getVersionGovernanceMode(), "ALL"),
                game.getVersionMin(),
                game.getVersionMax(),
                splitValues(game.getVersionBlocklist()),
                game.getGovernanceNote()
        );
    }

    private OpsGameGovernanceImpactDto buildGovernanceImpact(Game game) {
        List<String> channels = splitValues(game.getChannelGovernanceValues());
        List<String> regions = splitValues(game.getRegionGovernanceValues());
        List<String> blockedVersions = splitValues(game.getVersionBlocklist());
        String channelMode = safeMode(game.getChannelGovernanceMode(), "ALL");
        String regionMode = safeMode(game.getRegionGovernanceMode(), "ALL");
        String versionMode = safeMode(game.getVersionGovernanceMode(), "ALL");

        int activeControlCount = 0;
        List<String> highlights = new ArrayList<>();
        if (!"ALL".equals(channelMode) && !channels.isEmpty()) {
            activeControlCount++;
            highlights.add(("ALLOWLIST".equals(channelMode) ? "Channel allowlist: " : "Channel blocklist: ") + String.join(", ", channels));
        }
        if (!"ALL".equals(regionMode) && !regions.isEmpty()) {
            activeControlCount++;
            highlights.add(("ALLOWLIST".equals(regionMode) ? "Region allowlist: " : "Region blocklist: ") + String.join(", ", regions));
        }
        if (!"ALL".equals(versionMode)) {
            activeControlCount++;
            if ("MIN_VERSION".equals(versionMode)) {
                highlights.add("Minimum client version: " + safe(game.getVersionMin()));
            } else if ("RANGE".equals(versionMode)) {
                highlights.add("Client version range: " + safe(game.getVersionMin()) + " - " + safe(game.getVersionMax()));
            } else if ("BLOCKLIST".equals(versionMode)) {
                highlights.add("Blocked client versions: " + String.join(", ", blockedVersions));
            }
        }

        int affectedSurfaceCount = channels.size() + regions.size() + blockedVersions.size()
                + (game.getVersionMin() == null ? 0 : 1)
                + (game.getVersionMax() == null ? 0 : 1);

        String visibilityStatus = gameService.effectiveVisibilityStatus(game);
        String exposureState;
        if ("BLOCKED".equals(visibilityStatus) || "HIDDEN".equals(visibilityStatus)) {
            exposureState = "CLOSED";
        } else if (activeControlCount == 0) {
            exposureState = "FULL";
        } else {
            exposureState = "PARTIAL";
        }

        String impactLevel;
        if ("BLOCKED".equals(visibilityStatus)) {
            impactLevel = "CRITICAL";
        } else if ("HIDDEN".equals(visibilityStatus)) {
            impactLevel = "HIGH";
        } else if (activeControlCount >= 3 || affectedSurfaceCount >= 8) {
            impactLevel = "HIGH";
        } else if (activeControlCount == 2 || affectedSurfaceCount >= 4) {
            impactLevel = "MEDIUM";
        } else if (activeControlCount == 1) {
            impactLevel = "LOW";
        } else {
            impactLevel = "NONE";
        }

        String summary;
        if (highlights.isEmpty()) {
            summary = "No multidimensional governance rules configured";
        } else {
            summary = String.join(" | ", highlights);
        }

        return new OpsGameGovernanceImpactDto(
                game.getId(),
                game.getAppId(),
                game.getName(),
                visibilityStatus,
                exposureState,
                impactLevel,
                activeControlCount,
                affectedSurfaceCount,
                channels.size(),
                regions.size(),
                blockedVersions.size(),
                game.getVersionMin() != null,
                game.getVersionMax() != null,
                highlights,
                summary,
                game.getGovernanceNote()
        );
    }

    private String normalizeStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Game.GameStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String normalizeVisibilityStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "VISIBLE", "HIDDEN", "BLOCKED" -> normalized;
            default -> null;
        };
    }

    private String normalizeChannelRegionMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return "ALL";
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return CHANNEL_REGION_MODES.contains(normalized) ? normalized : null;
    }

    private String normalizeVersionMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return "ALL";
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return VERSION_MODES.contains(normalized) ? normalized : null;
    }

    private String normalizeCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.length() > 32) {
            return null;
        }
        return value;
    }

    private String normalizeKeyword(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.length() > 100) {
            return null;
        }
        return value;
    }

    private String normalizeBatchReviewAction(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return Set.of("ASSIGN", "APPROVE", "REJECT").contains(normalized) ? normalized : null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeMode(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    private List<String> splitValues(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .distinct()
                .toList();
    }

    private String joinValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(",", values);
    }

    private List<String> normalizeDimensionValues(List<String> values, int maxCount, int maxLength) {
        if (values == null) {
            return List.of();
        }
        List<String> normalized = values.stream()
                .filter(token -> token != null && !token.isBlank())
                .map(String::trim)
                .map(token -> token.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        if (normalized.size() > maxCount) {
            return null;
        }
        if (normalized.stream().anyMatch(token -> token.length() > maxLength)) {
            return null;
        }
        return normalized;
    }

    private String normalizeVersionToken(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 64 || !normalized.matches("[0-9A-Za-z._-]+")) {
            return null;
        }
        return normalized;
    }

    private int compareVersionTokens(String left, String right) {
        List<String> leftParts = Arrays.asList(left.split("[._-]"));
        List<String> rightParts = Arrays.asList(right.split("[._-]"));
        int size = Math.max(leftParts.size(), rightParts.size());
        for (int index = 0; index < size; index++) {
            String leftPart = index < leftParts.size() ? leftParts.get(index) : "0";
            String rightPart = index < rightParts.size() ? rightParts.get(index) : "0";
            int compare = compareVersionPart(leftPart, rightPart);
            if (compare != 0) {
                return compare;
            }
        }
        return 0;
    }

    private int compareVersionPart(String left, String right) {
        boolean leftNumeric = left.chars().allMatch(Character::isDigit);
        boolean rightNumeric = right.chars().allMatch(Character::isDigit);
        if (leftNumeric && rightNumeric) {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        }
        return left.compareToIgnoreCase(right);
    }
}
