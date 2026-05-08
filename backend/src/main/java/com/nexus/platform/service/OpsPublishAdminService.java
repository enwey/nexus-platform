package com.nexus.platform.service;

import com.nexus.platform.dto.OpsPublishOverviewDto;
import com.nexus.platform.dto.OpsPublishOrderDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.GameVersion;
import com.nexus.platform.entity.LaunchAd;
import com.nexus.platform.entity.OpsContentItem;
import com.nexus.platform.entity.OpsContentSlot;
import com.nexus.platform.repository.AuditLogRepository;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.GameVersionRepository;
import com.nexus.platform.repository.LaunchAdRepository;
import com.nexus.platform.repository.OpsContentItemRepository;
import com.nexus.platform.repository.OpsContentSlotRepository;
import com.nexus.platform.repository.OpsPublishOrderRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpsPublishAdminService {
    private final GameRepository gameRepository;
    private final GameVersionRepository gameVersionRepository;
    private final OpsContentItemRepository opsContentItemRepository;
    private final OpsContentSlotRepository opsContentSlotRepository;
    private final LaunchAdRepository launchAdRepository;
    private final AuditLogRepository auditLogRepository;
    private final OpsPublishOrderRepository opsPublishOrderRepository;

    public Result<OpsPublishOverviewDto> getOverview() {
        List<Game> games = gameRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, GameVersion> latestVersionsByGameId = gameVersionRepository.findAll().stream()
                .collect(Collectors.toMap(
                        GameVersion::getGameId,
                        Function.identity(),
                        (left, right) -> {
                            LocalDateTime leftUpdated = left.getUpdatedAt() == null ? left.getCreatedAt() : left.getUpdatedAt();
                            LocalDateTime rightUpdated = right.getUpdatedAt() == null ? right.getCreatedAt() : right.getUpdatedAt();
                            if (leftUpdated == null) {
                                return right;
                            }
                            if (rightUpdated == null) {
                                return left;
                            }
                            return rightUpdated.isAfter(leftUpdated) ? right : left;
                        }
                ));
        List<OpsContentItem> contentItems = opsContentItemRepository.findAll();
        Map<Long, OpsContentSlot> slotsById = opsContentSlotRepository.findAll().stream()
                .collect(Collectors.toMap(OpsContentSlot::getId, Function.identity()));
        Map<Long, Game> gameById = games.stream().collect(Collectors.toMap(Game::getId, Function.identity()));
        List<LaunchAd> launchAds = launchAdRepository.findAllByOrderByUpdatedAtDesc();
        List<OpsPublishOrderDto> publishOrders = opsPublishOrderRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .limit(20)
                .map(order -> new OpsPublishOrderDto(
                        order.getId(),
                        order.getAssetType(),
                        order.getTargetId(),
                        order.getAssetCode(),
                        order.getAssetName(),
                        order.getCurrentStatus(),
                        order.getDesiredAction(),
                        order.getRolloutMode(),
                        order.getRolloutPercent(),
                        order.getRolloutChannel(),
                        order.getOrderStatus(),
                        order.getScheduleAt(),
                        order.getExecutionNote(),
                        order.getRejectReason(),
                        order.getExecutionResult(),
                        order.getFailureReason(),
                        order.getExecutionReceipt(),
                        order.getRetryCount(),
                        order.getLastAttemptAt(),
                        order.getSubmittedBy(),
                        order.getApprovedBy(),
                        order.getPublishedBy(),
                        order.getCancelledBy(),
                        order.getSubmittedAt(),
                        order.getApprovedAt(),
                        order.getPublishedAt(),
                        order.getCancelledAt(),
                        order.getCreatedAt(),
                        order.getUpdatedAt()
                ))
                .toList();

        int pendingGameReviews = (int) games.stream().filter(game -> game.getStatus() == Game.GameStatus.PENDING).count();
        int blockedGames = (int) games.stream().filter(game -> "BLOCKED".equalsIgnoreCase(game.getVisibilityStatus())).count();
        int hiddenGames = (int) games.stream().filter(game -> "HIDDEN".equalsIgnoreCase(game.getVisibilityStatus())).count();
        int draftContentItems = (int) contentItems.stream().filter(item -> "DRAFT".equalsIgnoreCase(item.getStatus())).count();
        int publishedContentItems = (int) contentItems.stream().filter(item -> "PUBLISHED".equalsIgnoreCase(item.getStatus())).count();
        int pausedContentItems = (int) contentItems.stream().filter(item -> "PAUSED".equalsIgnoreCase(item.getStatus())).count();
        int draftLaunchAds = (int) launchAds.stream().filter(item -> "DRAFT".equalsIgnoreCase(item.getStatus())).count();
        int activeLaunchAds = (int) launchAds.stream().filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus())).count();

        List<OpsPublishOverviewDto.PendingPublishItem> pendingItems = new ArrayList<>();
        games.stream()
                .filter(game -> game.getStatus() == Game.GameStatus.PENDING)
                .limit(5)
                .forEach(game -> pendingItems.add(new OpsPublishOverviewDto.PendingPublishItem(
                        "GAME_REVIEW",
                        game.getAppId(),
                        game.getName(),
                        game.getStatus() == null ? "" : game.getStatus().name(),
                        "developer#" + game.getDeveloperId(),
                        game.getUpdatedAt()
                )));
        contentItems.stream()
                .filter(item -> "DRAFT".equalsIgnoreCase(item.getStatus()) || "PAUSED".equalsIgnoreCase(item.getStatus()))
                .sorted(Comparator.comparing(OpsContentItem::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .forEach(item -> pendingItems.add(new OpsPublishOverviewDto.PendingPublishItem(
                        "CONTENT_ITEM",
                        String.valueOf(item.getId()),
                        item.getTitle() == null || item.getTitle().isBlank() ? "Untitled Content Item" : item.getTitle(),
                        item.getStatus(),
                        "slot#" + item.getSlotId(),
                        item.getUpdatedAt()
                )));
        launchAds.stream()
                .filter(item -> "DRAFT".equalsIgnoreCase(item.getStatus()) || "INACTIVE".equalsIgnoreCase(item.getStatus()))
                .limit(5)
                .forEach(item -> pendingItems.add(new OpsPublishOverviewDto.PendingPublishItem(
                        "LAUNCH_AD",
                        item.getCode(),
                        item.getTitleZhCn(),
                        item.getStatus(),
                        item.getUpdatedBy(),
                        item.getUpdatedAt()
                )));

        List<OpsPublishOverviewDto.ReleaseGateItem> releaseGateItems = new ArrayList<>();
        games.stream()
                .map(game -> toGameReleaseGateItem(game, latestVersionsByGameId.get(game.getId())))
                .filter(item -> item != null)
                .forEach(releaseGateItems::add);
        contentItems.stream()
                .map(item -> toContentReleaseGateItem(item, slotsById.get(item.getSlotId()), gameById.get(item.getGameId())))
                .filter(item -> item != null)
                .forEach(releaseGateItems::add);
        launchAds.stream()
                .map(this::toLaunchAdReleaseGateItem)
                .filter(item -> item != null)
                .forEach(releaseGateItems::add);

        int readyToPublishAssets = (int) releaseGateItems.stream().filter(item -> "READY".equals(item.releaseState())).count();
        int blockedByRulesAssets = (int) releaseGateItems.stream().filter(item -> "BLOCKED".equals(item.releaseState())).count();
        int scheduledAssets = (int) releaseGateItems.stream().filter(item -> "SCHEDULED".equals(item.releaseState())).count();

        List<OpsPublishOverviewDto.RecentPublishAction> recentActions = auditLogRepository.findAllByOrderByCreatedAtDesc(
                        org.springframework.data.domain.PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC,
                                "createdAt"
                        ))
                )
                .stream()
                .filter(log -> log.getAction() != null && (
                        log.getAction().startsWith("DISCOVER_")
                                || log.getAction().startsWith("GAME_VISIBILITY_")
                                || log.getAction().startsWith("GAME_APPROVE")
                                || log.getAction().startsWith("GAME_REJECT")
                                || log.getAction().startsWith("LAUNCH_AD_")
                ))
                .limit(10)
                .map(log -> new OpsPublishOverviewDto.RecentPublishAction(
                        log.getAction(),
                        log.getTargetAppId(),
                        log.getReason(),
                        log.isSuccess(),
                        log.getCreatedAt()
                ))
                .toList();

        return Result.success(new OpsPublishOverviewDto(
                pendingGameReviews,
                blockedGames,
                hiddenGames,
                draftContentItems,
                publishedContentItems,
                pausedContentItems,
                draftLaunchAds,
                activeLaunchAds,
                readyToPublishAssets,
                blockedByRulesAssets,
                scheduledAssets,
                pendingItems.stream()
                        .sorted(Comparator.comparing(OpsPublishOverviewDto.PendingPublishItem::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(12)
                        .toList(),
                releaseGateItems.stream()
                        .sorted(Comparator.comparing(OpsPublishOverviewDto.ReleaseGateItem::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(20)
                        .toList(),
                publishOrders,
                recentActions
        ));
    }

    private OpsPublishOverviewDto.ReleaseGateItem toGameReleaseGateItem(Game game, GameVersion latestVersion) {
        if (game == null) {
            return null;
        }
        String visibility = game.getVisibilityStatus() == null || game.getVisibilityStatus().isBlank()
                ? "VISIBLE"
                : game.getVisibilityStatus().trim().toUpperCase();
        String latestVersionStatus = latestVersion == null || latestVersion.getStatus() == null
                ? "NO_VERSION"
                : latestVersion.getStatus().name();
        String currentStatus = (game.getStatus() == null ? "UNKNOWN" : game.getStatus().name()) + " / " + visibility;
        String releaseState;
        String action;
        String blockerReason = null;

        if (game.getStatus() == Game.GameStatus.PENDING) {
            releaseState = "BLOCKED";
            action = "WAIT_REVIEW";
            blockerReason = "Game is waiting for review approval";
        } else if (game.getStatus() == Game.GameStatus.PROCESSING) {
            releaseState = "BLOCKED";
            action = "WAIT_PACKAGE_PROCESS";
            blockerReason = "Game package is still processing";
        } else if (!"VISIBLE".equals(visibility)) {
            releaseState = "BLOCKED";
            action = "CHECK_VISIBILITY_CONTROL";
            blockerReason = game.getVisibilityReason() == null || game.getVisibilityReason().isBlank()
                    ? "Blocked by frontend visibility control"
                    : game.getVisibilityReason();
        } else if (game.getStatus() == Game.GameStatus.APPROVED) {
            releaseState = "LIVE";
            action = "MONITOR";
        } else if ("APPROVED".equals(latestVersionStatus)) {
            releaseState = "READY";
            action = "RESTORE_ONLINE_VERSION";
        } else {
            releaseState = "BLOCKED";
            action = "PREPARE_NEXT_VERSION";
            blockerReason = "No approved version is ready for production";
        }

        return new OpsPublishOverviewDto.ReleaseGateItem(
                "GAME",
                game.getId(),
                game.getAppId(),
                game.getName(),
                currentStatus,
                releaseState,
                action,
                blockerReason,
                "developer#" + game.getDeveloperId(),
                game.getUpdatedAt()
        );
    }

    private OpsPublishOverviewDto.ReleaseGateItem toContentReleaseGateItem(OpsContentItem item, OpsContentSlot slot, Game game) {
        if (item == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        String releaseState;
        String action;
        String blockerReason = null;
        if (slot != null && Boolean.FALSE.equals(slot.getEnabled())) {
            releaseState = "BLOCKED";
            action = "ENABLE_SLOT";
            blockerReason = "Placement slot is disabled";
        } else if (game == null || game.getStatus() != Game.GameStatus.APPROVED) {
            releaseState = "BLOCKED";
            action = "APPROVE_GAME_FIRST";
            blockerReason = "Linked game is not approved";
        } else if (game.getVisibilityStatus() != null && !"VISIBLE".equalsIgnoreCase(game.getVisibilityStatus())) {
            releaseState = "BLOCKED";
            action = "CLEAR_GAME_CONTROL";
            blockerReason = "Linked game is hidden or blocked";
        } else if ("PAUSED".equalsIgnoreCase(item.getStatus())) {
            releaseState = "BLOCKED";
            action = "RESUME_CONTENT";
            blockerReason = "Content item is paused";
        } else if ("DRAFT".equalsIgnoreCase(item.getStatus())) {
            releaseState = "READY";
            action = "PUBLISH_CONTENT";
        } else if ("PUBLISHED".equalsIgnoreCase(item.getStatus())
                && item.getStartAt() != null
                && item.getStartAt().isAfter(now)) {
            releaseState = "SCHEDULED";
            action = "WAIT_SCHEDULE";
        } else if ("PUBLISHED".equalsIgnoreCase(item.getStatus())
                && item.getEndAt() != null
                && item.getEndAt().isBefore(now)) {
            releaseState = "BLOCKED";
            action = "EXTEND_OR_REPLACE";
            blockerReason = "Content schedule has expired";
        } else {
            releaseState = "LIVE";
            action = "MONITOR";
        }

        return new OpsPublishOverviewDto.ReleaseGateItem(
                "CONTENT_ITEM",
                item.getId(),
                String.valueOf(item.getId()),
                item.getTitle() == null || item.getTitle().isBlank() ? "Untitled Content Item" : item.getTitle(),
                item.getStatus(),
                releaseState,
                action,
                blockerReason,
                slot == null ? "slot#" + item.getSlotId() : slot.getSlotCode(),
                item.getUpdatedAt()
        );
    }

    private OpsPublishOverviewDto.ReleaseGateItem toLaunchAdReleaseGateItem(LaunchAd item) {
        if (item == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        String effectiveState = deriveLaunchAdEffectiveState(item, now);
        String releaseState;
        String action;
        String blockerReason = null;
        if ("DRAFT".equalsIgnoreCase(item.getStatus())) {
            releaseState = "READY";
            action = "ACTIVATE_LAUNCH_AD";
        } else if ("INACTIVE".equalsIgnoreCase(item.getStatus())) {
            releaseState = "BLOCKED";
            action = "REACTIVATE_OR_ARCHIVE";
            blockerReason = "Launch ad is inactive";
        } else if ("SCHEDULED".equals(effectiveState)) {
            releaseState = "SCHEDULED";
            action = "WAIT_SCHEDULE";
        } else if ("EXPIRED".equals(effectiveState)) {
            releaseState = "BLOCKED";
            action = "EXTEND_OR_REPLACE";
            blockerReason = "Launch ad schedule has expired";
        } else {
            releaseState = "LIVE";
            action = "MONITOR";
        }

        return new OpsPublishOverviewDto.ReleaseGateItem(
                "LAUNCH_AD",
                item.getId(),
                item.getCode(),
                item.getTitleZhCn(),
                item.getStatus(),
                releaseState,
                action,
                blockerReason,
                item.getUpdatedBy(),
                item.getUpdatedAt()
        );
    }

    private String deriveLaunchAdEffectiveState(LaunchAd item, LocalDateTime now) {
        if (item.getStatus() == null || item.getStatus().isBlank()) {
            return "DRAFT";
        }
        String status = item.getStatus().trim().toUpperCase();
        if ("INACTIVE".equals(status)) {
            return "INACTIVE";
        }
        if (!"ACTIVE".equals(status)) {
            return "DRAFT";
        }
        if (item.getStartAt() != null && item.getStartAt().isAfter(now)) {
            return "SCHEDULED";
        }
        if (item.getEndAt() != null && item.getEndAt().isBefore(now)) {
            return "EXPIRED";
        }
        return "LIVE";
    }
}
