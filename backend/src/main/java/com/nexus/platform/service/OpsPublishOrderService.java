package com.nexus.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.dto.AuditFieldDiffDto;
import com.nexus.platform.dto.OpsPublishOrderCreateRequest;
import com.nexus.platform.dto.OpsPublishOrderDto;
import com.nexus.platform.dto.OpsPublishOrderPreviewDto;
import com.nexus.platform.dto.OpsPublishOrderStatusRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.GameVersion;
import com.nexus.platform.entity.LaunchAd;
import com.nexus.platform.entity.OpsContentItem;
import com.nexus.platform.entity.OpsMessageNotice;
import com.nexus.platform.entity.OpsPublishOrder;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.GameVersionRepository;
import com.nexus.platform.repository.LaunchAdRepository;
import com.nexus.platform.repository.OpsContentItemRepository;
import com.nexus.platform.repository.OpsMessageNoticeRepository;
import com.nexus.platform.repository.OpsPublishOrderRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OpsPublishOrderService {
    private static final Set<String> ASSET_TYPES = Set.of("CONTENT_ITEM", "LAUNCH_AD", "NOTICE", "GAME_VERSION_ROLLBACK");
    private static final Set<String> ORDER_STATUSES = Set.of("SUBMITTED", "APPROVED", "SCHEDULED", "REJECTED", "PUBLISHED", "CANCELLED", "FAILED");
    private static final Set<String> ROLLOUT_MODES = Set.of("FULL", "GRAY");

    private final OpsPublishOrderRepository opsPublishOrderRepository;
    private final OpsContentItemRepository opsContentItemRepository;
    private final LaunchAdRepository launchAdRepository;
    private final OpsMessageNoticeRepository opsMessageNoticeRepository;
    private final GameRepository gameRepository;
    private final GameVersionRepository gameVersionRepository;
    private final GameService gameService;
    private final AuditLogService auditLogService;
    private final AuditSnapshotService auditSnapshotService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public Result<List<OpsPublishOrderDto>> listOrders() {
        return Result.success(opsPublishOrderRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toDto).toList());
    }

    public Result<OpsPublishOrderPreviewDto> getOrderPreview(Long orderId) {
        if (orderId == null || orderId <= 0) {
            return Result.error("Invalid publish order id");
        }
        OpsPublishOrder order = opsPublishOrderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return Result.error("Publish order not found");
        }
        AssetSnapshot currentSnapshot = loadAssetSnapshot(order.getAssetType(), order.getTargetId(), null);
        if (currentSnapshot == null) {
            return Result.error("Publish target asset not found");
        }
        Map<String, Object> submittedMap = fromJson(order.getSnapshotJson());
        Map<String, Object> currentMap = fromJson(currentSnapshot.snapshotJson());
        Map<String, Object> desiredMap = readDesiredSnapshot(order, currentSnapshot, currentMap);
        List<OpsPublishOrderPreviewDto.FieldDiff> diffs = toPreviewDiffs(auditSnapshotService.buildDiffs(currentMap, desiredMap));
        List<OpsPublishOrderPreviewDto.FieldDiff> configDriftDiffs = toPreviewDiffs(auditSnapshotService.buildDiffs(submittedMap, currentMap));
        List<String> warnings = buildPreviewWarnings(order, currentSnapshot);
        Map<String, Object> executionBeforeSnapshot = fromJson(order.getExecutionBeforeSnapshotJson());
        Map<String, Object> executionAfterSnapshot = fromJson(order.getExecutionAfterSnapshotJson());
        List<OpsPublishOrderPreviewDto.FieldDiff> executionDiffs = toPreviewDiffs(
                auditSnapshotService.buildDiffs(executionBeforeSnapshot, executionAfterSnapshot)
        );
        return Result.success(new OpsPublishOrderPreviewDto(
                order.getId(),
                order.getAssetType(),
                order.getAssetCode(),
                order.getAssetName(),
                order.getDesiredAction(),
                order.getRolloutMode(),
                order.getRolloutPercent(),
                order.getRolloutChannel(),
                order.getOrderStatus(),
                order.getScheduleAt(),
                submittedMap,
                currentMap,
                desiredMap,
                diffs,
                configDriftDiffs,
                warnings,
                executionBeforeSnapshot,
                executionAfterSnapshot,
                executionDiffs,
                order.getExecutionResult(),
                order.getExecutionReceipt(),
                order.getFailureReason(),
                order.getRetryCount()
        ));
    }

    @Transactional
    public Result<OpsPublishOrderDto> createOrder(
            User currentUser,
            OpsPublishOrderCreateRequest request,
            String requestUri,
            String idempotencyKey
    ) {
        JavaType resultType = objectMapper.getTypeFactory().constructParametricType(Result.class, OpsPublishOrderDto.class);
        return idempotencyService.execute(
                "OPS_PUBLISH_ORDER_CREATE",
                idempotencyKey,
                request,
                currentUser == null ? null : currentUser.getId(),
                requestUri,
                resultType,
                () -> createOrderInternal(currentUser, request, requestUri)
        );
    }

    private Result<OpsPublishOrderDto> createOrderInternal(User currentUser, OpsPublishOrderCreateRequest request, String requestUri) {
        String assetType = normalizeEnum(request == null ? null : request.assetType(), ASSET_TYPES);
        if (assetType == null) {
            auditLogService.logOpsAudit("OPS_PUBLISH_ORDER_CREATE", currentUser, null, "PUBLISH_ORDER", false, "Invalid asset type", requestUri);
            return Result.error("Invalid asset type");
        }
        Long targetId = request == null ? null : request.targetId();
        if (targetId == null || targetId <= 0) {
            auditLogService.logOpsAudit("OPS_PUBLISH_ORDER_CREATE", currentUser, null, "PUBLISH_ORDER", false, "Invalid target id", requestUri);
            return Result.error("Invalid target id");
        }
        AssetSnapshot snapshot = loadAssetSnapshot(assetType, targetId, request == null ? null : request.relatedGameId());
        if (snapshot == null) {
            auditLogService.logOpsAudit("OPS_PUBLISH_ORDER_CREATE", currentUser, null, "PUBLISH_ORDER", false, "Asset not found", requestUri);
            return Result.error("Asset not found");
        }
        String desiredAction = validateDesiredAction(assetType, request == null ? null : request.desiredAction());
        if (desiredAction == null) {
            auditLogService.logOpsAudit("OPS_PUBLISH_ORDER_CREATE", currentUser, snapshot.targetGameId(), snapshot.assetCode(), false, "Invalid desired action", requestUri);
            return Result.error("Invalid desired action");
        }
        String rolloutMode = normalizeEnum(request == null ? null : request.rolloutMode(), ROLLOUT_MODES);
        if (rolloutMode == null) {
            rolloutMode = "FULL";
        }
        String rolloutValidation = validateRollout(assetType, desiredAction, rolloutMode, request == null ? null : request.rolloutPercent(), request == null ? null : request.rolloutChannel());
        if (rolloutValidation != null) {
            auditLogService.logOpsAudit("OPS_PUBLISH_ORDER_CREATE", currentUser, snapshot.targetGameId(), snapshot.assetCode(), false, rolloutValidation, requestUri);
            return Result.error(rolloutValidation);
        }
        if (request != null && request.scheduleAt() != null && request.scheduleAt().isBefore(LocalDateTime.now().minusMinutes(1))) {
            auditLogService.logOpsAudit("OPS_PUBLISH_ORDER_CREATE", currentUser, snapshot.targetGameId(), snapshot.assetCode(), false, "Schedule time cannot be earlier than now", requestUri);
            return Result.error("Schedule time cannot be earlier than now");
        }
        String validationMessage = validateAssetAction(snapshot, desiredAction, request == null ? null : request.scheduleAt());
        if (validationMessage != null) {
            auditLogService.logOpsAudit("OPS_PUBLISH_ORDER_CREATE", currentUser, snapshot.targetGameId(), snapshot.assetCode(), false, validationMessage, requestUri);
            return Result.error(validationMessage);
        }
        Map<String, Object> submittedSnapshot = fromJson(snapshot.snapshotJson());
        Map<String, Object> desiredSnapshot = projectDesiredSnapshot(snapshot, desiredAction, submittedSnapshot);

        OpsPublishOrder order = new OpsPublishOrder();
        order.setAssetType(assetType);
        order.setTargetId(targetId);
        order.setAssetCode(snapshot.assetCode());
        order.setAssetName(snapshot.assetName());
        order.setCurrentStatus(snapshot.currentStatus());
        order.setDesiredAction(desiredAction);
        order.setRolloutMode(rolloutMode);
        order.setRolloutPercent("GRAY".equals(rolloutMode) ? request.rolloutPercent() : null);
        order.setRolloutChannel("GRAY".equals(rolloutMode) ? trim(request.rolloutChannel(), 64) : null);
        order.setOrderStatus("SUBMITTED");
        order.setScheduleAt(request == null ? null : request.scheduleAt());
        order.setExecutionNote(trim(request == null ? null : request.executionNote(), 256));
        order.setExecutionResult("PENDING");
        order.setFailureReason(null);
        order.setExecutionReceipt(null);
        order.setRetryCount(0);
        order.setLastAttemptAt(null);
        order.setSnapshotJson(snapshot.snapshotJson());
        order.setDesiredSnapshotJson(toJson(desiredSnapshot));
        order.setConfigDiffJson(auditSnapshotService.toJson(auditSnapshotService.buildDiffs(submittedSnapshot, desiredSnapshot)));
        order.setSubmittedBy(currentUser.getId());
        order.setSubmittedAt(LocalDateTime.now());
        OpsPublishOrder saved = opsPublishOrderRepository.save(order);
        auditLogService.logOpsAudit(
                "OPS_PUBLISH_ORDER_CREATE",
                currentUser,
                snapshot.targetGameId(),
                snapshot.assetCode(),
                true,
                desiredAction,
                requestUri,
                "PUBLISH_ORDER_PLAN",
                submittedSnapshot,
                desiredSnapshot
        );
        return Result.success(toDto(saved));
    }

    @Transactional
    public Result<OpsPublishOrderDto> updateOrderStatus(
            Long orderId,
            User currentUser,
            OpsPublishOrderStatusRequest request,
            String requestUri,
            String idempotencyKey
    ) {
        JavaType resultType = objectMapper.getTypeFactory().constructParametricType(Result.class, OpsPublishOrderDto.class);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId);
        payload.put("request", request);
        return idempotencyService.execute(
                "OPS_PUBLISH_ORDER_STATUS_UPDATE",
                idempotencyKey,
                payload,
                currentUser == null ? null : currentUser.getId(),
                requestUri,
                resultType,
                () -> updateOrderStatusInternal(orderId, currentUser, request, requestUri)
        );
    }

    private Result<OpsPublishOrderDto> updateOrderStatusInternal(Long orderId, User currentUser, OpsPublishOrderStatusRequest request, String requestUri) {
        OpsPublishOrder order = opsPublishOrderRepository.findById(orderId).orElse(null);
        if (order == null) {
            auditLogService.logOpsAudit("OPS_PUBLISH_ORDER_STATUS_UPDATE", currentUser, null, "PUBLISH_ORDER", false, "Publish order not found", requestUri);
            return Result.error("Publish order not found");
        }
        String targetStatus = normalizeEnum(request == null ? null : request.orderStatus(), ORDER_STATUSES);
        if (targetStatus == null) {
            auditLogService.logOpsAudit("OPS_PUBLISH_ORDER_STATUS_UPDATE", currentUser, null, order.getAssetCode(), false, "Invalid publish order status", requestUri);
            return Result.error("Invalid publish order status");
        }
        String transitionError = validateTransition(order.getOrderStatus(), targetStatus);
        if (transitionError != null) {
            auditLogService.logOpsAudit("OPS_PUBLISH_ORDER_STATUS_UPDATE", currentUser, null, order.getAssetCode(), false, transitionError, requestUri);
            return Result.error(transitionError);
        }
        String reason = trim(request == null ? null : request.reason(), 256);
        if ("REJECTED".equals(targetStatus) && reason == null) {
            auditLogService.logOpsAudit("OPS_PUBLISH_ORDER_STATUS_UPDATE", currentUser, null, order.getAssetCode(), false, "Reject reason is required", requestUri);
            return Result.error("Reject reason is required");
        }
        Map<String, Object> beforeOrderSnapshot = buildOrderStateSnapshot(order);
        if ("APPROVED".equals(targetStatus)) {
            order.setApprovedBy(currentUser.getId());
            order.setApprovedAt(LocalDateTime.now());
            if (order.getScheduleAt() != null && order.getScheduleAt().isAfter(LocalDateTime.now())) {
                order.setOrderStatus("SCHEDULED");
                OpsPublishOrder saved = opsPublishOrderRepository.save(order);
                auditLogService.logOpsAudit(
                        "OPS_PUBLISH_ORDER_STATUS_UPDATE",
                        currentUser,
                        null,
                        order.getAssetCode(),
                        true,
                        "SCHEDULED",
                        requestUri,
                        "PUBLISH_ORDER_STATUS",
                        beforeOrderSnapshot,
                        buildOrderStateSnapshot(saved)
                );
                return Result.success(toDto(saved));
            }
        } else if ("SCHEDULED".equals(targetStatus)) {
            if (order.getScheduleAt() == null || !order.getScheduleAt().isAfter(LocalDateTime.now())) {
                auditLogService.logOpsAudit("OPS_PUBLISH_ORDER_STATUS_UPDATE", currentUser, null, order.getAssetCode(), false, "Schedule time is required for scheduled orders", requestUri);
                return Result.error("Schedule time is required for scheduled orders");
            }
            if (order.getApprovedBy() == null) {
                order.setApprovedBy(currentUser.getId());
            }
            if (order.getApprovedAt() == null) {
                order.setApprovedAt(LocalDateTime.now());
            }
        } else if ("REJECTED".equals(targetStatus)) {
            order.setRejectReason(reason);
        } else if ("PUBLISHED".equals(targetStatus)) {
            String publishError = executeOrder(order, currentUser, requestUri);
            if (publishError != null) {
                markExecutionFailure(order, currentUser, publishError, requestUri);
                OpsPublishOrder failed = opsPublishOrderRepository.save(order);
                auditLogService.logOpsAudit(
                        "OPS_PUBLISH_ORDER_STATUS_UPDATE",
                        currentUser,
                        null,
                        order.getAssetCode(),
                        false,
                        publishError,
                        requestUri,
                        "PUBLISH_ORDER_STATUS",
                        beforeOrderSnapshot,
                        buildOrderStateSnapshot(failed)
                );
                return Result.success(toDto(failed));
            }
            order.setPublishedBy(currentUser.getId());
            order.setPublishedAt(LocalDateTime.now());
            markExecutionSuccess(order);
        } else if ("CANCELLED".equals(targetStatus)) {
            order.setCancelledBy(currentUser.getId());
            order.setCancelledAt(LocalDateTime.now());
        }
        order.setOrderStatus(targetStatus);
        OpsPublishOrder saved = opsPublishOrderRepository.save(order);
        auditLogService.logOpsAudit(
                "OPS_PUBLISH_ORDER_STATUS_UPDATE",
                currentUser,
                null,
                order.getAssetCode(),
                true,
                targetStatus,
                requestUri,
                "PUBLISH_ORDER_STATUS",
                beforeOrderSnapshot,
                buildOrderStateSnapshot(saved)
        );
        return Result.success(toDto(saved));
    }

    @Transactional
    public Result<List<OpsPublishOrderDto>> executeDueScheduledOrders(User currentUser, String requestUri, String idempotencyKey) {
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, OpsPublishOrderDto.class);
        JavaType resultType = objectMapper.getTypeFactory().constructParametricType(Result.class, listType);
        return idempotencyService.execute(
                "OPS_PUBLISH_ORDER_EXECUTE_DUE",
                idempotencyKey,
                Map.of("action", "EXECUTE_DUE"),
                currentUser == null ? null : currentUser.getId(),
                requestUri,
                resultType,
                () -> executeDueScheduledOrdersInternal(currentUser, requestUri)
        );
    }

    private Result<List<OpsPublishOrderDto>> executeDueScheduledOrdersInternal(User currentUser, String requestUri) {
        List<OpsPublishOrder> dueOrders = opsPublishOrderRepository
                .findByOrderStatusAndScheduleAtLessThanEqualOrderByScheduleAtAsc("SCHEDULED", LocalDateTime.now());
        for (OpsPublishOrder order : dueOrders) {
            String publishError = executeOrder(order, currentUser, requestUri);
            if (publishError != null) {
                markExecutionFailure(order, currentUser, publishError, requestUri);
                auditLogService.logOpsAudit("OPS_PUBLISH_ORDER_EXECUTE_DUE", currentUser, null, order.getAssetCode(), false, publishError, requestUri);
                continue;
            }
            order.setOrderStatus("PUBLISHED");
            order.setPublishedBy(currentUser.getId());
            order.setPublishedAt(LocalDateTime.now());
            markExecutionSuccess(order);
        }
        opsPublishOrderRepository.saveAll(dueOrders);
        if (!dueOrders.isEmpty()) {
            auditLogService.logOpsAudit("OPS_PUBLISH_ORDER_EXECUTE_DUE", currentUser, null, "PUBLISH_ORDER", true, "Executed due scheduled orders: " + dueOrders.size(), requestUri);
        }
        return Result.success(dueOrders.stream().map(this::toDto).toList());
    }

    private String executeOrder(OpsPublishOrder order, User currentUser, String requestUri) {
        AssetSnapshot snapshot = loadAssetSnapshot(order.getAssetType(), order.getTargetId(), null);
        if (snapshot == null) {
            return "Asset not found";
        }
        Map<String, Object> beforeExecutionSnapshot = fromJson(snapshot.snapshotJson());
        order.setExecutionBeforeSnapshotJson(toJson(beforeExecutionSnapshot));
        String validationMessage = validateAssetAction(snapshot, order.getDesiredAction(), order.getScheduleAt());
        if (validationMessage != null) {
            return validationMessage;
        }
        switch (order.getAssetType()) {
            case "CONTENT_ITEM" -> {
                OpsContentItem item = opsContentItemRepository.findById(order.getTargetId()).orElse(null);
                if (item == null) return "Content item not found";
                if ("PUBLISH".equals(order.getDesiredAction())) {
                    item.setStatus("PUBLISHED");
                } else if ("PAUSE".equals(order.getDesiredAction())) {
                    item.setStatus("PAUSED");
                } else if ("DRAFT".equals(order.getDesiredAction())) {
                    item.setStatus("DRAFT");
                }
                opsContentItemRepository.save(item);
                Map<String, Object> afterExecutionSnapshot = fromJson(loadAssetSnapshot("CONTENT_ITEM", item.getId(), null).snapshotJson());
                order.setExecutionAfterSnapshotJson(toJson(afterExecutionSnapshot));
                auditLogService.logOpsAudit(
                        "OPS_PUBLISH_EXECUTE_CONTENT",
                        currentUser,
                        item.getGameId(),
                        String.valueOf(item.getId()),
                        true,
                        buildExecutionAuditReason(order),
                        requestUri,
                        "PUBLISH_ORDER_EXECUTION",
                        beforeExecutionSnapshot,
                        afterExecutionSnapshot
                );
            }
            case "LAUNCH_AD" -> {
                LaunchAd item = launchAdRepository.findById(order.getTargetId()).orElse(null);
                if (item == null) return "Launch ad not found";
                if ("ACTIVATE".equals(order.getDesiredAction())) {
                    item.setStatus("ACTIVE");
                } else if ("DEACTIVATE".equals(order.getDesiredAction())) {
                    item.setStatus("INACTIVE");
                } else if ("DRAFT".equals(order.getDesiredAction())) {
                    item.setStatus("DRAFT");
                }
                item.setUpdatedBy(currentUser.getUsername());
                launchAdRepository.save(item);
                Map<String, Object> afterExecutionSnapshot = fromJson(loadAssetSnapshot("LAUNCH_AD", item.getId(), null).snapshotJson());
                order.setExecutionAfterSnapshotJson(toJson(afterExecutionSnapshot));
                auditLogService.logOpsAudit(
                        "OPS_PUBLISH_EXECUTE_LAUNCH_AD",
                        currentUser,
                        null,
                        item.getCode(),
                        true,
                        buildExecutionAuditReason(order),
                        requestUri,
                        "PUBLISH_ORDER_EXECUTION",
                        beforeExecutionSnapshot,
                        afterExecutionSnapshot
                );
            }
            case "NOTICE" -> {
                OpsMessageNotice item = opsMessageNoticeRepository.findById(order.getTargetId()).orElse(null);
                if (item == null) return "Notice not found";
                if ("PUBLISH".equals(order.getDesiredAction())) {
                    item.setDeliveryStatus("PUBLISHED");
                } else if ("PAUSE".equals(order.getDesiredAction())) {
                    item.setDeliveryStatus("PAUSED");
                } else if ("DRAFT".equals(order.getDesiredAction())) {
                    item.setDeliveryStatus("DRAFT");
                }
                opsMessageNoticeRepository.save(item);
                Map<String, Object> afterExecutionSnapshot = fromJson(loadAssetSnapshot("NOTICE", item.getId(), null).snapshotJson());
                order.setExecutionAfterSnapshotJson(toJson(afterExecutionSnapshot));
                auditLogService.logOpsAudit(
                        "OPS_PUBLISH_EXECUTE_NOTICE",
                        currentUser,
                        null,
                        "NOTICE:" + item.getId(),
                        true,
                        buildExecutionAuditReason(order),
                        requestUri,
                        "PUBLISH_ORDER_EXECUTION",
                        beforeExecutionSnapshot,
                        afterExecutionSnapshot
                );
            }
            case "GAME_VERSION_ROLLBACK" -> {
                GameVersion version = gameVersionRepository.findById(order.getTargetId()).orElse(null);
                if (version == null) return "Game version not found";
                Result<Void> rollbackResult = gameService.rollbackToVersion(
                        version.getGameId(),
                        version.getId(),
                        currentUser,
                        requestUri,
                        order.getExecutionNote()
                );
                if (rollbackResult.getCode() != 0) {
                    return rollbackResult.getMessage();
                }
                Map<String, Object> afterExecutionSnapshot = fromJson(
                        loadAssetSnapshot("GAME_VERSION_ROLLBACK", version.getId(), version.getGameId()).snapshotJson()
                );
                order.setExecutionAfterSnapshotJson(toJson(afterExecutionSnapshot));
                auditLogService.logOpsAudit(
                        "OPS_PUBLISH_EXECUTE_GAME_ROLLBACK",
                        currentUser,
                        version.getGameId(),
                        "VERSION:" + version.getId(),
                        true,
                        buildExecutionAuditReason(order),
                        requestUri,
                        "PUBLISH_ORDER_EXECUTION",
                        beforeExecutionSnapshot,
                        afterExecutionSnapshot
                );
            }
            default -> {
                return "Unsupported asset type";
            }
        }
        order.setConfigDiffJson(auditSnapshotService.toJson(
                auditSnapshotService.buildDiffs(fromJson(order.getSnapshotJson()), fromJson(order.getExecutionAfterSnapshotJson()))
        ));
        return null;
    }

    private AssetSnapshot loadAssetSnapshot(String assetType, Long targetId, Long relatedGameId) {
        return switch (assetType) {
            case "CONTENT_ITEM" -> opsContentItemRepository.findById(targetId)
                    .map(item -> {
                        Game game = gameRepository.findById(item.getGameId()).orElse(null);
                        Map<String, Object> snapshot = new LinkedHashMap<>();
                        snapshot.put("id", item.getId());
                        snapshot.put("status", item.getStatus());
                        snapshot.put("gameId", item.getGameId());
                        snapshot.put("title", item.getTitle());
                        snapshot.put("startAt", item.getStartAt());
                        snapshot.put("endAt", item.getEndAt());
                        return new AssetSnapshot(
                                "CONTENT_ITEM",
                                String.valueOf(item.getId()),
                                item.getTitle() == null || item.getTitle().isBlank() ? "Untitled Content Item" : item.getTitle(),
                                item.getStatus(),
                                game == null ? null : game.getId(),
                                toJson(snapshot),
                                null,
                                null,
                                null
                        );
                    }).orElse(null);
            case "LAUNCH_AD" -> launchAdRepository.findById(targetId)
                    .map(item -> {
                        Map<String, Object> snapshot = new LinkedHashMap<>();
                        snapshot.put("id", item.getId());
                        snapshot.put("status", item.getStatus());
                        snapshot.put("code", item.getCode());
                        snapshot.put("titleZhCn", item.getTitleZhCn());
                        snapshot.put("startAt", item.getStartAt());
                        snapshot.put("endAt", item.getEndAt());
                        return new AssetSnapshot(
                                "LAUNCH_AD",
                                item.getCode(),
                                item.getTitleZhCn(),
                                item.getStatus(),
                                null,
                                toJson(snapshot),
                                null,
                                null,
                                null
                        );
                    }).orElse(null);
            case "NOTICE" -> opsMessageNoticeRepository.findById(targetId)
                    .map(item -> {
                        Map<String, Object> snapshot = new LinkedHashMap<>();
                        snapshot.put("id", item.getId());
                        snapshot.put("status", item.getDeliveryStatus());
                        snapshot.put("audienceRole", item.getAudienceRole());
                        snapshot.put("title", item.getTitle());
                        snapshot.put("startAt", item.getStartAt());
                        snapshot.put("endAt", item.getEndAt());
                        return new AssetSnapshot(
                                "NOTICE",
                                "NOTICE:" + item.getId(),
                                item.getTitle(),
                                item.getDeliveryStatus(),
                                null,
                                toJson(snapshot),
                                null,
                                null,
                                null
                        );
                    }).orElse(null);
            case "GAME_VERSION_ROLLBACK" -> gameVersionRepository.findById(targetId)
                    .map(version -> {
                        Game game = gameRepository.findById(version.getGameId()).orElse(null);
                        if (game == null) {
                            return null;
                        }
                        if (relatedGameId != null && !relatedGameId.equals(game.getId())) {
                            return null;
                        }
                        Map<String, Object> snapshot = new LinkedHashMap<>();
                        snapshot.put("versionId", version.getId());
                        snapshot.put("gameId", game.getId());
                        snapshot.put("appId", game.getAppId());
                        snapshot.put("targetVersion", version.getVersionName());
                        snapshot.put("currentVersion", game.getVersion());
                        snapshot.put("versionStatus", version.getStatus() == null ? null : version.getStatus().name());
                        return new AssetSnapshot(
                                "GAME_VERSION_ROLLBACK",
                                game.getAppId() + "@" + version.getVersionName(),
                                game.getName() + " / " + version.getVersionName(),
                                game.getVersion(),
                                game.getId(),
                                toJson(snapshot),
                                version.getId(),
                                version.getVersionName(),
                                game.getVersion()
                        );
                    }).orElse(null);
            default -> null;
        };
    }

    private String validateDesiredAction(String assetType, String desiredAction) {
        String normalized = desiredAction == null ? null : desiredAction.trim().toUpperCase(Locale.ROOT);
        if (normalized == null) {
            return null;
        }
        return switch (assetType) {
            case "CONTENT_ITEM", "NOTICE" -> Set.of("PUBLISH", "PAUSE", "DRAFT").contains(normalized) ? normalized : null;
            case "LAUNCH_AD" -> Set.of("ACTIVATE", "DEACTIVATE", "DRAFT").contains(normalized) ? normalized : null;
            case "GAME_VERSION_ROLLBACK" -> "ROLLBACK_PUBLISH".equals(normalized) ? normalized : null;
            default -> null;
        };
    }

    private String validateAssetAction(AssetSnapshot snapshot, String desiredAction, LocalDateTime scheduleAt) {
        return switch (snapshot.assetType()) {
            case "CONTENT_ITEM" -> validateContentItemAction(snapshot, desiredAction);
            case "LAUNCH_AD" -> validateLaunchAdAction(snapshot, desiredAction);
            case "NOTICE" -> validateNoticeAction(snapshot, desiredAction);
            case "GAME_VERSION_ROLLBACK" -> validateRollbackAction(snapshot, desiredAction);
            default -> "Unsupported asset type";
        };
    }

    private String validateContentItemAction(AssetSnapshot snapshot, String desiredAction) {
        if ("PUBLISH".equals(desiredAction) && "PUBLISHED".equalsIgnoreCase(snapshot.currentStatus())) {
            return "Content item is already published";
        }
        if ("PAUSE".equals(desiredAction) && !"PUBLISHED".equalsIgnoreCase(snapshot.currentStatus())) {
            return "Only published content items can be paused";
        }
        return null;
    }

    private String validateLaunchAdAction(AssetSnapshot snapshot, String desiredAction) {
        if ("ACTIVATE".equals(desiredAction) && "ACTIVE".equalsIgnoreCase(snapshot.currentStatus())) {
            return "Launch ad is already active";
        }
        if ("DEACTIVATE".equals(desiredAction) && !"ACTIVE".equalsIgnoreCase(snapshot.currentStatus())) {
            return "Only active launch ads can be deactivated";
        }
        return null;
    }

    private String validateNoticeAction(AssetSnapshot snapshot, String desiredAction) {
        if ("PUBLISH".equals(desiredAction) && "PUBLISHED".equalsIgnoreCase(snapshot.currentStatus())) {
            return "Notice is already published";
        }
        if ("PAUSE".equals(desiredAction) && !"PUBLISHED".equalsIgnoreCase(snapshot.currentStatus())) {
            return "Only published notices can be paused";
        }
        return null;
    }

    private String validateRollbackAction(AssetSnapshot snapshot, String desiredAction) {
        if (!"ROLLBACK_PUBLISH".equals(desiredAction)) {
            return "Unsupported rollback action";
        }
        if (snapshot.rollbackVersionId() == null) {
            return "Rollback version not found";
        }
        GameVersion version = gameVersionRepository.findById(snapshot.rollbackVersionId()).orElse(null);
        if (version == null || version.getStatus() != GameVersion.VersionStatus.APPROVED) {
            return "Only approved versions can be used for rollback";
        }
        String targetVersion = snapshot.rollbackTargetVersion();
        String currentVersion = snapshot.currentLiveVersion();
        if (targetVersion != null && targetVersion.equals(currentVersion)) {
            return "Target version is already online";
        }
        return null;
    }

    private String validateTransition(String currentStatus, String targetStatus) {
        if (currentStatus == null || currentStatus.isBlank()) {
            return "Current publish order status is invalid";
        }
        String normalizedCurrent = currentStatus.trim().toUpperCase(Locale.ROOT);
        if (normalizedCurrent.equals(targetStatus)) {
            return null;
        }
        return switch (normalizedCurrent) {
            case "SUBMITTED" -> Set.of("APPROVED", "SCHEDULED", "REJECTED", "CANCELLED").contains(targetStatus) ? null : "Submitted orders can only be approved, scheduled, rejected, or cancelled";
            case "APPROVED" -> Set.of("PUBLISHED", "SCHEDULED", "CANCELLED").contains(targetStatus) ? null : "Approved orders can only be published, scheduled, or cancelled";
            case "SCHEDULED" -> Set.of("PUBLISHED", "CANCELLED").contains(targetStatus) ? null : "Scheduled orders can only be published or cancelled";
            case "FAILED" -> Set.of("PUBLISHED", "CANCELLED").contains(targetStatus) ? null : "Failed orders can only be retried or cancelled";
            default -> "Terminal publish orders cannot be changed";
        };
    }

    private OpsPublishOrderDto toDto(OpsPublishOrder order) {
        return new OpsPublishOrderDto(
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
        );
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
        } catch (Exception exception) {
            return new LinkedHashMap<>();
        }
    }

    private Map<String, Object> projectDesiredSnapshot(AssetSnapshot snapshot, String desiredAction, Map<String, Object> current) {
        Map<String, Object> desired = new LinkedHashMap<>(current);
        switch (snapshot.assetType()) {
            case "CONTENT_ITEM" -> {
                if ("PUBLISH".equals(desiredAction)) desired.put("status", "PUBLISHED");
                if ("PAUSE".equals(desiredAction)) desired.put("status", "PAUSED");
                if ("DRAFT".equals(desiredAction)) desired.put("status", "DRAFT");
            }
            case "LAUNCH_AD" -> {
                if ("ACTIVATE".equals(desiredAction)) desired.put("status", "ACTIVE");
                if ("DEACTIVATE".equals(desiredAction)) desired.put("status", "INACTIVE");
                if ("DRAFT".equals(desiredAction)) desired.put("status", "DRAFT");
            }
            case "NOTICE" -> {
                if ("PUBLISH".equals(desiredAction)) desired.put("status", "PUBLISHED");
                if ("PAUSE".equals(desiredAction)) desired.put("status", "PAUSED");
                if ("DRAFT".equals(desiredAction)) desired.put("status", "DRAFT");
            }
            case "GAME_VERSION_ROLLBACK" -> {
                desired.put("currentVersion", snapshot.rollbackTargetVersion());
                desired.put("rollbackVersionId", snapshot.rollbackVersionId());
                desired.put("rollbackAction", desiredAction);
            }
            default -> {
            }
        }
        return desired;
    }

    private List<String> buildPreviewWarnings(OpsPublishOrder order, AssetSnapshot snapshot) {
        List<String> warnings = new ArrayList<>();
        String validation = validateAssetAction(snapshot, order.getDesiredAction(), order.getScheduleAt());
        if (validation != null) {
            warnings.add(validation);
        }
        if (order.getScheduleAt() != null) {
            warnings.add("This order will not take effect until the scheduled time is reached and executed");
        }
        if ("GRAY".equals(order.getRolloutMode())) {
            warnings.add("This publish order will be executed as a gray release and should be coordinated with runtime traffic controls");
        }
        if ("CANCELLED".equals(order.getOrderStatus()) || "REJECTED".equals(order.getOrderStatus())) {
            warnings.add("This publish order is already terminal and cannot be executed directly");
        }
        if ("GAME_VERSION_ROLLBACK".equals(order.getAssetType())) {
            warnings.add("Rollback publishing will directly replace the currently online version");
        }
        return warnings;
    }

    private String stringify(Object value) {
        if (value == null) {
            return "-";
        }
        String normalized = String.valueOf(value);
        return normalized.isBlank() ? "-" : normalized;
    }

    private String validateRollout(String assetType, String desiredAction, String rolloutMode, Integer rolloutPercent, String rolloutChannel) {
        if (!ROLLOUT_MODES.contains(rolloutMode)) {
            return "Invalid rollout mode";
        }
        if ("FULL".equals(rolloutMode)) {
            return null;
        }
        if ("GAME_VERSION_ROLLBACK".equals(assetType)) {
            return "Gray rollout is not supported for rollback publish orders";
        }
        if ("PAUSE".equals(desiredAction) || "DEACTIVATE".equals(desiredAction) || "DRAFT".equals(desiredAction)) {
            return "Gray rollout is only supported for go-live publish actions";
        }
        if (rolloutPercent == null || rolloutPercent < 1 || rolloutPercent > 99) {
            return "Gray rollout percent must be between 1 and 99";
        }
        if (trim(rolloutChannel, 64) == null) {
            return "Gray rollout channel is required";
        }
        return null;
    }

    private String buildExecutionAuditReason(OpsPublishOrder order) {
        if (!"GRAY".equals(order.getRolloutMode())) {
            return order.getDesiredAction();
        }
        return order.getDesiredAction() + " / GRAY " + order.getRolloutPercent() + "% / " + stringify(order.getRolloutChannel());
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

    private String toJson(Map<String, Object> snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private Map<String, Object> readDesiredSnapshot(OpsPublishOrder order, AssetSnapshot currentSnapshot, Map<String, Object> currentMap) {
        Map<String, Object> desiredSnapshot = fromJson(order.getDesiredSnapshotJson());
        if (desiredSnapshot.isEmpty()) {
            return projectDesiredSnapshot(currentSnapshot, order.getDesiredAction(), currentMap);
        }
        return desiredSnapshot;
    }

    private List<OpsPublishOrderPreviewDto.FieldDiff> toPreviewDiffs(List<AuditFieldDiffDto> source) {
        return source.stream()
                .map(item -> new OpsPublishOrderPreviewDto.FieldDiff(item.field(), item.beforeValue(), item.afterValue()))
                .toList();
    }

    private Map<String, Object> buildOrderStateSnapshot(OpsPublishOrder order) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("orderStatus", order.getOrderStatus());
        snapshot.put("desiredAction", order.getDesiredAction());
        snapshot.put("rolloutMode", order.getRolloutMode());
        snapshot.put("rolloutPercent", order.getRolloutPercent());
        snapshot.put("rolloutChannel", order.getRolloutChannel());
        snapshot.put("scheduleAt", order.getScheduleAt());
        snapshot.put("executionResult", order.getExecutionResult());
        snapshot.put("failureReason", order.getFailureReason());
        snapshot.put("retryCount", order.getRetryCount());
        snapshot.put("approvedAt", order.getApprovedAt());
        snapshot.put("publishedAt", order.getPublishedAt());
        snapshot.put("cancelledAt", order.getCancelledAt());
        return snapshot;
    }

    private void markExecutionSuccess(OpsPublishOrder order) {
        order.setExecutionResult("SUCCESS");
        order.setFailureReason(null);
        order.setExecutionReceipt("Executed at " + LocalDateTime.now() + " with action " + order.getDesiredAction());
        order.setLastAttemptAt(LocalDateTime.now());
    }

    private void markExecutionFailure(OpsPublishOrder order, User currentUser, String failureReason, String requestUri) {
        order.setOrderStatus("FAILED");
        order.setExecutionResult("FAILED");
        order.setFailureReason(trim(failureReason, 512));
        order.setExecutionReceipt("Last failure at " + LocalDateTime.now() + ": " + trim(failureReason, 256));
        order.setLastAttemptAt(LocalDateTime.now());
        order.setRetryCount((order.getRetryCount() == null ? 0 : order.getRetryCount()) + 1);
        auditLogService.logOpsAudit("OPS_PUBLISH_ORDER_EXECUTION_FAILED", currentUser, null, order.getAssetCode(), false, failureReason, requestUri);
    }

    private record AssetSnapshot(
            String assetType,
            String assetCode,
            String assetName,
            String currentStatus,
            Long targetGameId,
            String snapshotJson,
            Long rollbackVersionId,
            String rollbackTargetVersion,
            String currentLiveVersion
    ) {
    }
}
