package com.nexus.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.dto.OpsDiscoverBatchStatusRequest;
import com.nexus.platform.dto.OpsDiscoverBatchOperationRequest;
import com.nexus.platform.dto.OpsDiscoverCategoryRequest;
import com.nexus.platform.dto.OpsDiscoverCategoryResponse;
import com.nexus.platform.dto.OpsDiscoverAnalyticsDto;
import com.nexus.platform.dto.OpsDiscoverConfigResponse;
import com.nexus.platform.dto.OpsDiscoverConfigUpdateRequest;
import com.nexus.platform.dto.OpsDiscoverExperimentRequest;
import com.nexus.platform.dto.OpsDiscoverExperimentStatusRequest;
import com.nexus.platform.dto.OpsDiscoverPublishOrderCreateRequest;
import com.nexus.platform.dto.OpsDiscoverPublishOrderStatusRequest;
import com.nexus.platform.dto.OpsDiscoverPublishPreviewResponse;
import com.nexus.platform.dto.OpsDiscoverSlotUpdateRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.OpsCollection;
import com.nexus.platform.entity.OpsCollectionGameRel;
import com.nexus.platform.entity.OpsContentItem;
import com.nexus.platform.entity.OpsContentSlot;
import com.nexus.platform.entity.OpsDiscoverCategory;
import com.nexus.platform.entity.OpsDiscoverExperiment;
import com.nexus.platform.entity.OpsDiscoverPublishOrder;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.OpsCollectionGameRelRepository;
import com.nexus.platform.repository.OpsCollectionRepository;
import com.nexus.platform.repository.OpsContentItemRepository;
import com.nexus.platform.repository.OpsContentSlotRepository;
import com.nexus.platform.repository.OpsDiscoverCategoryRepository;
import com.nexus.platform.repository.OpsDiscoverExperimentRepository;
import com.nexus.platform.repository.OpsDiscoverPublishOrderRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpsDiscoverAdminService {
    private static final String SLOT_DISCOVER_HERO = "DISCOVER_HERO";
    private static final String SLOT_LIBRARY_COLDSTART_HERO = "LIBRARY_COLDSTART_HERO";
    private static final String SLOT_DISCOVER_RANK = "DISCOVER_RANK";
    private static final String SLOT_COMMUNITY_TODAY = "COMMUNITY_TODAY";
    private static final String COLLECTION_NEWBIE_MUST_PLAY = "NEWBIE_MUST_PLAY";
    private static final String COLLECTION_EVERYONE_PLAYING = "EVERYONE_PLAYING";
    private static final String CATEGORY_STATUS_ENABLED = "ENABLED";
    private static final String DEFAULT_COMMUNITY_CATEGORY_NAME = "編輯精選";
    private static final String SCOPE_DISCOVER_BANNERS = "DISCOVER_BANNERS";
    private static final String SCOPE_GAME_BANNERS = "GAME_BANNERS";
    private static final String SCOPE_COMMUNITY = "COMMUNITY";
    private static final String ORDER_STATUS_PUBLISHED = "PUBLISHED";
    private static final String ORDER_STATUS_SCHEDULED = "SCHEDULED";
    private static final String ORDER_STATUS_CANCELLED = "CANCELLED";
    private static final String EXPERIMENT_STATUS_ACTIVE = "ACTIVE";
    private static final String EXPERIMENT_STATUS_PAUSED = "PAUSED";
    private static final String EXPERIMENT_STATUS_DRAFT = "DRAFT";

    private final GameRepository gameRepository;
    private final OpsContentSlotRepository slotRepository;
    private final OpsContentItemRepository itemRepository;
    private final OpsCollectionRepository collectionRepository;
    private final OpsCollectionGameRelRepository collectionGameRelRepository;
    private final OpsDiscoverCategoryRepository categoryRepository;
    private final OpsDiscoverPublishOrderRepository publishOrderRepository;
    private final OpsDiscoverExperimentRepository experimentRepository;
    private final GameService gameService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Result<OpsDiscoverConfigResponse> getConfig() {
        List<Game> approvedGames = gameRepository.findByStatusOrderByCreatedAtDesc(Game.GameStatus.APPROVED);
        List<Game> visibleApprovedGames = approvedGames.stream()
                .filter(gameService::isFrontendVisibleForPublic)
                .toList();
        Map<Long, Game> gameById = approvedGames.stream().collect(Collectors.toMap(Game::getId, Function.identity()));
        ensureDefaultCategorySeed();

        OpsContentSlot heroSlot = ensureSlot(SLOT_DISCOVER_HERO, "Discover Hero", "DISCOVER", "HERO");
        OpsContentSlot libraryTopBannerSlot =
                ensureSlot(SLOT_LIBRARY_COLDSTART_HERO, "Library Coldstart Hero", "LIBRARY", "TOP_BANNER");
        OpsContentSlot rankSlot = ensureSlot(SLOT_DISCOVER_RANK, "Discover Rank", "DISCOVER", "RANK");
        OpsContentSlot communitySlot = ensureSlot(SLOT_COMMUNITY_TODAY, "Community Today", "COMMUNITY", "TODAY");
        OpsCollection newbieCollection = ensureCollection(COLLECTION_NEWBIE_MUST_PLAY, "Newbie Must Play");
        OpsCollection everyoneCollection = ensureCollection(COLLECTION_EVERYONE_PLAYING, "Everyone Playing");

        List<OpsContentItem> discoverTopRows = itemRepository.findBySlotIdOrderBySortOrderAscUpdatedAtDesc(heroSlot.getId());
        List<OpsContentItem> gameTopRows =
                itemRepository.findBySlotIdOrderBySortOrderAscUpdatedAtDesc(libraryTopBannerSlot.getId());
        OpsContentItem heroItem = discoverTopRows.stream().findFirst().orElse(null);
        OpsDiscoverConfigResponse.HeroConfig hero = null;
        if (heroItem != null) {
            Game heroGame = gameById.get(heroItem.getGameId());
            hero = new OpsDiscoverConfigResponse.HeroConfig(
                    heroGame == null ? "" : heroGame.getAppId(),
                    heroItem.getTitle(),
                    heroItem.getSubtitle(),
                    heroItem.getBadgeText(),
                    heroItem.getCoverUrl()
            );
        }
        List<OpsDiscoverConfigResponse.TopBannerConfig> discoverTopBanners = discoverTopRows.stream()
                .map(item -> {
                    Game game = gameById.get(item.getGameId());
                    return new OpsDiscoverConfigResponse.TopBannerConfig(
                            item.getId(),
                            game == null ? "" : game.getAppId(),
                            item.getTitle(),
                            item.getSubtitle(),
                            item.getBadgeText(),
                            item.getCoverUrl(),
                            item.getStatus(),
                            item.getStartAt(),
                            item.getEndAt(),
                            item.getSortOrder()
                    );
                })
                .toList();
        List<OpsDiscoverConfigResponse.TopBannerConfig> gameTopBanners = gameTopRows.stream()
                .map(item -> {
                    Game game = gameById.get(item.getGameId());
                    return new OpsDiscoverConfigResponse.TopBannerConfig(
                            item.getId(),
                            game == null ? "" : game.getAppId(),
                            item.getTitle(),
                            item.getSubtitle(),
                            item.getBadgeText(),
                            item.getCoverUrl(),
                            item.getStatus(),
                            item.getStartAt(),
                            item.getEndAt(),
                            item.getSortOrder()
                    );
                })
                .toList();

        List<String> ranked = itemRepository.findBySlotIdOrderBySortOrderAscUpdatedAtDesc(rankSlot.getId()).stream()
                .map(item -> gameById.get(item.getGameId()))
                .filter(game -> game != null)
                .map(Game::getAppId)
                .toList();

        List<String> newbie = collectionGameRelRepository
                .findByCollectionIdOrderByPinTopDescSortOrderAscUpdatedAtDesc(newbieCollection.getId()).stream()
                .map(rel -> gameById.get(rel.getGameId()))
                .filter(game -> game != null)
                .map(Game::getAppId)
                .toList();

        List<String> everyone = collectionGameRelRepository
                .findByCollectionIdOrderByPinTopDescSortOrderAscUpdatedAtDesc(everyoneCollection.getId()).stream()
                .map(rel -> gameById.get(rel.getGameId()))
                .filter(game -> game != null)
                .map(Game::getAppId)
                .toList();

        List<OpsContentItem> communityRows = itemRepository.findBySlotIdOrderBySortOrderAscUpdatedAtDesc(communitySlot.getId());
        if (communityRows.isEmpty()) {
            seedDefaultCommunityItems(communitySlot.getId(), visibleApprovedGames);
            communityRows = itemRepository.findBySlotIdOrderBySortOrderAscUpdatedAtDesc(communitySlot.getId());
        }

        List<OpsDiscoverConfigResponse.CommunityItemConfig> communityItems =
                communityRows.stream()
                        .map(item -> {
                            Game game = gameById.get(item.getGameId());
                            return new OpsDiscoverConfigResponse.CommunityItemConfig(
                                    item.getId(),
                                    game == null ? "" : game.getAppId(),
                                    item.getBadgeText(),
                                    item.getTitle(),
                                    item.getCoverUrl(),
                                    item.getArticleTag(),
                                    item.getArticleTitle(),
                                    item.getArticleBody(),
                                    item.getActionText(),
                                    item.getStatus(),
                                    item.getStartAt(),
                                    item.getEndAt(),
                                    item.getSortOrder()
                            );
                        })
                        .toList();

        List<OpsDiscoverConfigResponse.SimpleGameItem> availableGames = visibleApprovedGames.stream()
                .map(game -> new OpsDiscoverConfigResponse.SimpleGameItem(
                        game.getId(),
                        game.getAppId(),
                        game.getName(),
                        game.getCategory(),
                        game.getStatus() == null ? "" : game.getStatus().name()
                ))
                .toList();

        List<OpsDiscoverConfigResponse.CategoryOption> categoryOptions = buildCategoryOptions();
        List<OpsDiscoverConfigResponse.ScopePreview> previews = List.of(
                buildScopePreview(SCOPE_DISCOVER_BANNERS, discoverTopBanners, resolveLiveTopBannerScope(SCOPE_DISCOVER_BANNERS)),
                buildScopePreview(SCOPE_GAME_BANNERS, gameTopBanners, resolveLiveTopBannerScope(SCOPE_GAME_BANNERS)),
                buildScopePreview(SCOPE_COMMUNITY, communityItems, resolveLiveCommunityScope())
        );
        return Result.success(new OpsDiscoverConfigResponse(
                hero,
                gameTopBanners,
                discoverTopBanners,
                ranked,
                newbie,
                everyone,
                communityItems,
                availableGames,
                categoryOptions,
                buildSlotControls(List.of(heroSlot, libraryTopBannerSlot, rankSlot, communitySlot)),
                previews,
                listRecentPublishOrders(),
                listRecentExperiments()
        ));
    }

    @Transactional
    public Result<OpsDiscoverAnalyticsDto> getAnalytics() {
        List<OpsDiscoverAnalyticsDto.ScopeMetrics> scopes = List.of(
                buildScopeAnalytics(SCOPE_DISCOVER_BANNERS, buildDraftTopBannerScope(SCOPE_DISCOVER_BANNERS)),
                buildScopeAnalytics(SCOPE_GAME_BANNERS, buildDraftTopBannerScope(SCOPE_GAME_BANNERS)),
                buildScopeAnalytics(SCOPE_COMMUNITY, buildDraftCommunityScope())
        );
        return Result.success(new OpsDiscoverAnalyticsDto(
                scopes.stream().mapToInt(OpsDiscoverAnalyticsDto.ScopeMetrics::draftItems).sum(),
                scopes.stream().mapToInt(OpsDiscoverAnalyticsDto.ScopeMetrics::liveItems).sum(),
                scopes.stream().mapToInt(OpsDiscoverAnalyticsDto.ScopeMetrics::scheduledItems).sum(),
                scopes.stream().mapToInt(OpsDiscoverAnalyticsDto.ScopeMetrics::pausedItems).sum(),
                scopes.stream().mapToInt(OpsDiscoverAnalyticsDto.ScopeMetrics::expiredItems).sum(),
                scopes.stream().mapToInt(OpsDiscoverAnalyticsDto.ScopeMetrics::pendingPublishOrders).sum(),
                scopes.stream().mapToInt(OpsDiscoverAnalyticsDto.ScopeMetrics::activeExperiments).sum(),
                scopes.stream().mapToInt(OpsDiscoverAnalyticsDto.ScopeMetrics::scheduledExperiments).sum(),
                scopes.stream().mapToInt(OpsDiscoverAnalyticsDto.ScopeMetrics::experimentTrafficPercent).sum(),
                scopes.stream()
                        .map(OpsDiscoverAnalyticsDto.ScopeMetrics::nextPublishAt)
                        .filter(Objects::nonNull)
                        .min(LocalDateTime::compareTo)
                        .orElse(null),
                scopes
        ));
    }

    @Transactional
    public Result<OpsDiscoverConfigResponse> updateConfig(
            OpsDiscoverConfigUpdateRequest request,
            User currentUser,
            String requestUri
    ) {
        if (request == null) {
            auditLogService.logOpsAudit("DISCOVER_CONFIG_SAVE", currentUser, null, null, false, "Request body is required", requestUri);
            return Result.error("Request body is required");
        }
        ensureDefaultCategorySeed();

        List<Game> games = gameRepository.findByStatusOrderByCreatedAtDesc(Game.GameStatus.APPROVED);
        Map<String, Game> gameByAppId = games.stream()
                .filter(gameService::isFrontendVisibleForPublic)
                .collect(Collectors.toMap(Game::getAppId, Function.identity()));

        OpsContentSlot heroSlot = ensureSlot(SLOT_DISCOVER_HERO, "Discover Hero", "DISCOVER", "HERO");
        OpsContentSlot libraryTopBannerSlot =
                ensureSlot(SLOT_LIBRARY_COLDSTART_HERO, "Library Coldstart Hero", "LIBRARY", "TOP_BANNER");
        OpsContentSlot rankSlot = ensureSlot(SLOT_DISCOVER_RANK, "Discover Rank", "DISCOVER", "RANK");
        OpsContentSlot communitySlot = ensureSlot(SLOT_COMMUNITY_TODAY, "Community Today", "COMMUNITY", "TODAY");
        OpsCollection newbieCollection = ensureCollection(COLLECTION_NEWBIE_MUST_PLAY, "Newbie Must Play");
        OpsCollection everyoneCollection = ensureCollection(COLLECTION_EVERYONE_PLAYING, "Everyone Playing");
        Set<String> validCategoryNames = categoryRepository.findByStatusOrderBySortOrderAscUpdatedAtDesc(CATEGORY_STATUS_ENABLED)
                .stream()
                .map(OpsDiscoverCategory::getName)
                .collect(Collectors.toSet());

        itemRepository.deleteBySlotId(libraryTopBannerSlot.getId());
        String saveGameTopBannerError = saveTopBannerItems(
                libraryTopBannerSlot.getId(),
                request.gameTopBanners(),
                gameByAppId,
                validCategoryNames
        );
        if (saveGameTopBannerError != null) {
            auditLogService.logOpsAudit("DISCOVER_CONFIG_SAVE", currentUser, null, null, false, saveGameTopBannerError, requestUri);
            return Result.error(saveGameTopBannerError);
        }

        itemRepository.deleteBySlotId(heroSlot.getId());
        List<OpsDiscoverConfigUpdateRequest.TopBannerConfig> discoverTopBanners = request.discoverTopBanners();
        if ((discoverTopBanners == null || discoverTopBanners.isEmpty())
                && request.hero() != null
                && request.hero().appId() != null
                && !request.hero().appId().isBlank()) {
            discoverTopBanners = List.of(new OpsDiscoverConfigUpdateRequest.TopBannerConfig(
                    request.hero().appId(),
                    request.hero().title(),
                    request.hero().subtitle(),
                    request.hero().badgeText(),
                    request.hero().coverUrl(),
                    "PUBLISHED",
                    null,
                    null,
                    0
            ));
        }
        String saveDiscoverTopBannerError = saveTopBannerItems(
                heroSlot.getId(),
                discoverTopBanners,
                gameByAppId,
                validCategoryNames
        );
        if (saveDiscoverTopBannerError != null) {
            auditLogService.logOpsAudit("DISCOVER_CONFIG_SAVE", currentUser, null, null, false, saveDiscoverTopBannerError, requestUri);
            return Result.error(saveDiscoverTopBannerError);
        }

        itemRepository.deleteBySlotId(rankSlot.getId());
        saveSlotItems(rankSlot.getId(), request.rankedAppIds(), gameByAppId);
        itemRepository.deleteBySlotId(communitySlot.getId());
        String saveCommunityError =
                saveCommunityItems(communitySlot.getId(), request.communityItems(), gameByAppId, validCategoryNames);
        if (saveCommunityError != null) {
            auditLogService.logOpsAudit("DISCOVER_CONFIG_SAVE", currentUser, null, null, false, saveCommunityError, requestUri);
            return Result.error(saveCommunityError);
        }

        replaceCollectionRelations(newbieCollection, request.newbieAppIds(), gameByAppId, currentUser);
        replaceCollectionRelations(everyoneCollection, request.everyoneAppIds(), gameByAppId, currentUser);

        auditLogService.logOpsAudit("DISCOVER_CONFIG_SAVE", currentUser, null, null, true, "Discover config saved", requestUri);
        return getConfig();
    }

    @Transactional
    public Result<OpsDiscoverConfigResponse> batchUpdateContentItemStatus(
            OpsDiscoverBatchStatusRequest request,
            User currentUser,
            String requestUri
    ) {
        if (request == null || request.itemIds() == null || request.itemIds().isEmpty()) {
            auditLogService.logOpsAudit("DISCOVER_ITEM_BATCH_STATUS", currentUser, null, null, false, "Item ids are required", requestUri);
            return Result.error("Item ids are required");
        }
        if (request.itemIds().size() > 100) {
            auditLogService.logOpsAudit("DISCOVER_ITEM_BATCH_STATUS", currentUser, null, null, false, "Too many item ids", requestUri);
            return Result.error("Too many item ids");
        }
        String status = normalizePlacementStatus(request.status());
        if (status == null) {
            auditLogService.logOpsAudit("DISCOVER_ITEM_BATCH_STATUS", currentUser, null, null, false, "Invalid status", requestUri);
            return Result.error("Invalid status");
        }

        List<Long> uniqueIds = request.itemIds().stream().filter(id -> id != null && id > 0).distinct().toList();
        if (uniqueIds.isEmpty()) {
            auditLogService.logOpsAudit("DISCOVER_ITEM_BATCH_STATUS", currentUser, null, null, false, "No valid item ids", requestUri);
            return Result.error("No valid item ids");
        }
        List<OpsContentItem> items = itemRepository.findByIdIn(uniqueIds);
        if (items.size() != uniqueIds.size()) {
            auditLogService.logOpsAudit("DISCOVER_ITEM_BATCH_STATUS", currentUser, null, null, false, "Some items do not exist", requestUri);
            return Result.error("Some items do not exist");
        }

        Set<Long> managedSlotIds = Set.of(
                ensureSlot(SLOT_DISCOVER_HERO, "Discover Hero", "DISCOVER", "HERO").getId(),
                ensureSlot(SLOT_LIBRARY_COLDSTART_HERO, "Library Coldstart Hero", "LIBRARY", "TOP_BANNER").getId(),
                ensureSlot(SLOT_COMMUNITY_TODAY, "Community Today", "COMMUNITY", "TODAY").getId()
        );
        Set<String> validCategoryNames = categoryRepository.findByStatusOrderBySortOrderAscUpdatedAtDesc(CATEGORY_STATUS_ENABLED)
                .stream()
                .map(OpsDiscoverCategory::getName)
                .collect(Collectors.toSet());

        Map<Long, Game> gameById = gameRepository.findByStatusOrderByCreatedAtDesc(Game.GameStatus.APPROVED)
                .stream()
                .collect(Collectors.toMap(Game::getId, Function.identity()));

        for (OpsContentItem item : items) {
            if (!managedSlotIds.contains(item.getSlotId())) {
                auditLogService.logOpsAudit("DISCOVER_ITEM_BATCH_STATUS", currentUser, null, null, false, "Unsupported slot item", requestUri);
                return Result.error("Unsupported slot item");
            }
            String validationError = validateContentItemForStatusChange(item, status, gameById, validCategoryNames);
            if (validationError != null) {
                auditLogService.logOpsAudit(
                        "DISCOVER_ITEM_BATCH_STATUS",
                        currentUser,
                        item.getGameId(),
                        gameById.get(item.getGameId()) == null ? null : gameById.get(item.getGameId()).getAppId(),
                        false,
                        validationError,
                        requestUri
                );
                return Result.error(validationError);
            }
        }

        for (OpsContentItem item : items) {
            item.setStatus(status);
        }
        itemRepository.saveAll(items);

        for (OpsContentItem item : items) {
            Game game = gameById.get(item.getGameId());
            auditLogService.logOpsAudit(
                    "DISCOVER_ITEM_BATCH_STATUS",
                    currentUser,
                    item.getGameId(),
                    game == null ? null : game.getAppId(),
                    true,
                    "Set status to " + status + " for content item " + item.getId(),
                    requestUri
            );
        }
        return getConfig();
    }

    @Transactional
    public Result<OpsDiscoverConfigResponse> batchOperate(
            OpsDiscoverBatchOperationRequest request,
            User currentUser,
            String requestUri
    ) {
        String targetType = normalizeBatchTarget(request == null ? null : request.targetType());
        if (targetType == null) {
            auditLogService.logOpsAudit("DISCOVER_BATCH_OPERATION", currentUser, null, null, false, "Target type is invalid", requestUri);
            return Result.error("Target type is invalid");
        }
        List<Long> ids = request == null || request.ids() == null
                ? List.of()
                : request.ids().stream().filter(id -> id != null && id > 0).distinct().toList();
        if (ids.isEmpty() || ids.size() > 100) {
            auditLogService.logOpsAudit("DISCOVER_BATCH_OPERATION", currentUser, null, null, false, "Batch size is invalid", requestUri);
            return Result.error("Batch size is invalid");
        }
        String reason = trimToNull(request.reason());
        if (reason != null && reason.length() > 255) {
            auditLogService.logOpsAudit("DISCOVER_BATCH_OPERATION", currentUser, null, null, false, "Reason is too long", requestUri);
            return Result.error("Reason is too long");
        }

        if ("PUBLISH_ORDER".equals(targetType)) {
            if (!ORDER_STATUS_CANCELLED.equals(normalizeOrderStatus(request.status()))) {
                auditLogService.logOpsAudit("DISCOVER_BATCH_OPERATION", currentUser, null, null, false, "Only cancellation is supported for publish orders", requestUri);
                return Result.error("Only cancellation is supported for publish orders");
            }
            List<OpsDiscoverPublishOrder> orders = publishOrderRepository.findAllById(ids);
            if (orders.size() != ids.size() || orders.stream().anyMatch(order -> !ORDER_STATUS_SCHEDULED.equals(order.getStatus()))) {
                auditLogService.logOpsAudit("DISCOVER_BATCH_OPERATION", currentUser, null, null, false, "Only scheduled publish orders can be cancelled in batch", requestUri);
                return Result.error("Only scheduled publish orders can be cancelled in batch");
            }
            orders.forEach(order -> order.setStatus(ORDER_STATUS_CANCELLED));
            publishOrderRepository.saveAll(orders);
            for (OpsDiscoverPublishOrder order : orders) {
                auditLogService.logOpsAudit(
                        "DISCOVER_PUBLISH_ORDER_BATCH_STATUS",
                        currentUser,
                        null,
                        order.getOrderNo(),
                        true,
                        "Cancelled publish order in batch" + (reason == null ? "" : ": " + reason),
                        requestUri
                );
            }
            return getConfig();
        }

        String experimentStatus = normalizeExperimentStatus(request.status());
        if (experimentStatus == null) {
            auditLogService.logOpsAudit("DISCOVER_BATCH_OPERATION", currentUser, null, null, false, "Experiment status is invalid", requestUri);
            return Result.error("Experiment status is invalid");
        }
        List<OpsDiscoverExperiment> experiments = experimentRepository.findAllById(ids);
        if (experiments.size() != ids.size()) {
            auditLogService.logOpsAudit("DISCOVER_BATCH_OPERATION", currentUser, null, null, false, "Some experiments were not found", requestUri);
            return Result.error("Some experiments were not found");
        }
        experiments.forEach(experiment -> experiment.setStatus(experimentStatus));
        experimentRepository.saveAll(experiments);
        for (OpsDiscoverExperiment experiment : experiments) {
            auditLogService.logOpsAudit(
                    "DISCOVER_EXPERIMENT_BATCH_STATUS",
                    currentUser,
                    null,
                    experiment.getExperimentName(),
                    true,
                    "Updated experiment to " + experimentStatus + (reason == null ? "" : ": " + reason),
                    requestUri
            );
        }
        return getConfig();
    }

    @Transactional
    public Result<OpsDiscoverConfigResponse> updateSlotControl(
            String slotCode,
            OpsDiscoverSlotUpdateRequest request,
            User currentUser,
            String requestUri
    ) {
        if (slotCode == null || slotCode.isBlank()) {
            auditLogService.logOpsAudit("DISCOVER_SLOT_CONTROL_UPDATE", currentUser, null, null, false, "Slot code is required", requestUri);
            return Result.error("Slot code is required");
        }
        if (request == null || request.enabled() == null) {
            auditLogService.logOpsAudit("DISCOVER_SLOT_CONTROL_UPDATE", currentUser, null, null, false, "Enabled flag is required", requestUri);
            return Result.error("Enabled flag is required");
        }

        String normalizedSlotCode = slotCode.trim().toUpperCase();
        if (!isManagedSlotCode(normalizedSlotCode)) {
            auditLogService.logOpsAudit("DISCOVER_SLOT_CONTROL_UPDATE", currentUser, null, null, false, "Slot is not operable", requestUri);
            return Result.error("Slot is not operable");
        }

        String reason = trimToNull(request.reason());
        if (!request.enabled() && (reason == null || reason.length() < 4)) {
            auditLogService.logOpsAudit("DISCOVER_SLOT_CONTROL_UPDATE", currentUser, null, null, false, "Disable reason is required", requestUri);
            return Result.error("Disable reason is required");
        }
        if (reason != null && reason.length() > 200) {
            auditLogService.logOpsAudit("DISCOVER_SLOT_CONTROL_UPDATE", currentUser, null, null, false, "Reason is too long", requestUri);
            return Result.error("Reason is too long");
        }

        OpsContentSlot slot = slotRepository.findBySlotCode(normalizedSlotCode).orElse(null);
        if (slot == null) {
            auditLogService.logOpsAudit("DISCOVER_SLOT_CONTROL_UPDATE", currentUser, null, null, false, "Slot not found", requestUri);
            return Result.error("Slot not found");
        }

        slot.setEnabled(request.enabled());
        slotRepository.save(slot);
        auditLogService.logOpsAudit(
                "DISCOVER_SLOT_CONTROL_UPDATE",
                currentUser,
                null,
                null,
                true,
                "Slot " + normalizedSlotCode + " set to " + (request.enabled() ? "ENABLED" : "DISABLED")
                        + (reason == null ? "" : " reason: " + reason),
                requestUri
        );
        return getConfig();
    }

    @Transactional
    public Result<OpsDiscoverPublishPreviewResponse> previewPublishScope(String scopeCode) {
        String normalizedScope = normalizeScopeCode(scopeCode);
        if (normalizedScope == null) {
            return Result.error("Scope code is invalid");
        }
        return Result.success(buildPublishPreview(normalizedScope));
    }

    @Transactional
    public Result<OpsDiscoverConfigResponse> createPublishOrder(
            OpsDiscoverPublishOrderCreateRequest request,
            User currentUser,
            String requestUri
    ) {
        String normalizedScope = normalizeScopeCode(request == null ? null : request.scopeCode());
        if (normalizedScope == null) {
            auditLogService.logOpsAudit("DISCOVER_PUBLISH_ORDER_CREATE", currentUser, null, null, false, "Scope code is invalid", requestUri);
            return Result.error("Scope code is invalid");
        }
        LocalDateTime effectiveAt = request == null || request.effectiveAt() == null ? LocalDateTime.now() : request.effectiveAt();
        String reason = trimToNull(request == null ? null : request.reason());
        if (reason != null && reason.length() > 255) {
            auditLogService.logOpsAudit("DISCOVER_PUBLISH_ORDER_CREATE", currentUser, null, null, false, "Reason is too long", requestUri);
            return Result.error("Reason is too long");
        }

        OpsDiscoverPublishPreviewResponse preview = buildPublishPreview(normalizedScope);
        if (preview.draftCount() == null || preview.draftCount() == 0) {
            auditLogService.logOpsAudit("DISCOVER_PUBLISH_ORDER_CREATE", currentUser, null, null, false, "Nothing to publish", requestUri);
            return Result.error("Nothing to publish");
        }

        OpsDiscoverPublishOrder order = new OpsDiscoverPublishOrder();
        order.setOrderNo("DOP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setScopeCode(normalizedScope);
        order.setScopeName(scopeLabel(normalizedScope));
        order.setStatus(effectiveAt.isAfter(LocalDateTime.now()) ? ORDER_STATUS_SCHEDULED : ORDER_STATUS_PUBLISHED);
        order.setPayloadJson(serializeScopePayload(normalizedScope));
        order.setEffectiveAt(effectiveAt);
        order.setReason(reason);
        order.setCreatedByUserId(currentUser == null ? null : currentUser.getId());
        order.setCreatedByName(currentUser == null ? null : currentUser.getUsername());
        publishOrderRepository.save(order);

        auditLogService.logOpsAudit(
                "DISCOVER_PUBLISH_ORDER_CREATE",
                currentUser,
                null,
                null,
                true,
                "Created publish order " + order.getOrderNo() + " for " + normalizedScope + " effective at " + effectiveAt,
                requestUri
        );
        return getConfig();
    }

    @Transactional
    public Result<OpsDiscoverConfigResponse> updatePublishOrderStatus(
            Long id,
            OpsDiscoverPublishOrderStatusRequest request,
            User currentUser,
            String requestUri
    ) {
        if (id == null) {
            auditLogService.logOpsAudit("DISCOVER_PUBLISH_ORDER_STATUS", currentUser, null, null, false, "Order id is required", requestUri);
            return Result.error("Order id is required");
        }
        OpsDiscoverPublishOrder order = publishOrderRepository.findById(id).orElse(null);
        if (order == null) {
            auditLogService.logOpsAudit("DISCOVER_PUBLISH_ORDER_STATUS", currentUser, null, null, false, "Order not found", requestUri);
            return Result.error("Order not found");
        }
        String status = normalizeOrderStatus(request == null ? null : request.status());
        if (!ORDER_STATUS_CANCELLED.equals(status)) {
            auditLogService.logOpsAudit("DISCOVER_PUBLISH_ORDER_STATUS", currentUser, null, null, false, "Only cancellation is supported", requestUri);
            return Result.error("Only cancellation is supported");
        }
        order.setStatus(ORDER_STATUS_CANCELLED);
        publishOrderRepository.save(order);
        auditLogService.logOpsAudit(
                "DISCOVER_PUBLISH_ORDER_STATUS",
                currentUser,
                null,
                null,
                true,
                "Cancelled publish order " + order.getOrderNo(),
                requestUri
        );
        return getConfig();
    }

    @Transactional
    public Result<OpsDiscoverConfigResponse> saveExperiment(
            OpsDiscoverExperimentRequest request,
            User currentUser,
            String requestUri
    ) {
        String normalizedScope = normalizeScopeCode(request == null ? null : request.scopeCode());
        if (normalizedScope == null) {
            auditLogService.logOpsAudit("DISCOVER_EXPERIMENT_SAVE", currentUser, null, null, false, "Scope code is invalid", requestUri);
            return Result.error("Scope code is invalid");
        }
        String experimentName = trimToNull(request.experimentName());
        if (experimentName == null || experimentName.length() < 4 || experimentName.length() > 128) {
            auditLogService.logOpsAudit("DISCOVER_EXPERIMENT_SAVE", currentUser, null, null, false, "Experiment name is invalid", requestUri);
            return Result.error("Experiment name is invalid");
        }
        Integer trafficPercent = request.trafficPercent();
        if (trafficPercent == null || trafficPercent < 1 || trafficPercent > 100) {
            auditLogService.logOpsAudit("DISCOVER_EXPERIMENT_SAVE", currentUser, null, null, false, "Traffic percent is invalid", requestUri);
            return Result.error("Traffic percent is invalid");
        }
        String status = normalizeExperimentStatus(request.status());
        if (status == null) {
            auditLogService.logOpsAudit("DISCOVER_EXPERIMENT_SAVE", currentUser, null, null, false, "Experiment status is invalid", requestUri);
            return Result.error("Experiment status is invalid");
        }
        if (!isValidSchedule(request.startAt(), request.endAt())) {
            auditLogService.logOpsAudit("DISCOVER_EXPERIMENT_SAVE", currentUser, null, null, false, "Experiment schedule is invalid", requestUri);
            return Result.error("Experiment schedule is invalid");
        }
        String note = trimToNull(request.note());
        if (note != null && note.length() > 255) {
            auditLogService.logOpsAudit("DISCOVER_EXPERIMENT_SAVE", currentUser, null, null, false, "Experiment note is too long", requestUri);
            return Result.error("Experiment note is too long");
        }

        OpsDiscoverExperiment experiment = request.id() == null
                ? new OpsDiscoverExperiment()
                : experimentRepository.findById(request.id()).orElse(null);
        if (request.id() != null && experiment == null) {
            auditLogService.logOpsAudit("DISCOVER_EXPERIMENT_SAVE", currentUser, null, null, false, "Experiment not found", requestUri);
            return Result.error("Experiment not found");
        }

        experiment.setSlotCode(scopeSlotCode(normalizedScope));
        experiment.setScopeCode(normalizedScope);
        experiment.setExperimentName(experimentName);
        experiment.setTrafficPercent(trafficPercent);
        experiment.setStatus(status);
        experiment.setStartAt(request.startAt());
        experiment.setEndAt(request.endAt());
        experiment.setNote(note);
        experiment.setVariantPayloadJson(serializeScopePayload(normalizedScope));
        experiment.setCreatedByUserId(experiment.getCreatedByUserId() == null && currentUser != null ? currentUser.getId() : experiment.getCreatedByUserId());
        experiment.setCreatedByName(experiment.getCreatedByName() == null && currentUser != null ? currentUser.getUsername() : experiment.getCreatedByName());
        experimentRepository.save(experiment);

        auditLogService.logOpsAudit(
                "DISCOVER_EXPERIMENT_SAVE",
                currentUser,
                null,
                null,
                true,
                "Saved experiment " + experimentName + " for " + normalizedScope,
                requestUri
        );
        return getConfig();
    }

    @Transactional
    public Result<OpsDiscoverConfigResponse> updateExperimentStatus(
            Long id,
            OpsDiscoverExperimentStatusRequest request,
            User currentUser,
            String requestUri
    ) {
        if (id == null) {
            auditLogService.logOpsAudit("DISCOVER_EXPERIMENT_STATUS", currentUser, null, null, false, "Experiment id is required", requestUri);
            return Result.error("Experiment id is required");
        }
        OpsDiscoverExperiment experiment = experimentRepository.findById(id).orElse(null);
        if (experiment == null) {
            auditLogService.logOpsAudit("DISCOVER_EXPERIMENT_STATUS", currentUser, null, null, false, "Experiment not found", requestUri);
            return Result.error("Experiment not found");
        }
        String status = normalizeExperimentStatus(request == null ? null : request.status());
        if (status == null) {
            auditLogService.logOpsAudit("DISCOVER_EXPERIMENT_STATUS", currentUser, null, null, false, "Experiment status is invalid", requestUri);
            return Result.error("Experiment status is invalid");
        }
        experiment.setStatus(status);
        experimentRepository.save(experiment);
        auditLogService.logOpsAudit(
                "DISCOVER_EXPERIMENT_STATUS",
                currentUser,
                null,
                null,
                true,
                "Updated experiment " + experiment.getExperimentName() + " to " + status,
                requestUri
        );
        return getConfig();
    }

    @Transactional
    public Result<List<OpsDiscoverCategoryResponse>> listCategories() {
        ensureDefaultCategorySeed();
        List<OpsDiscoverCategoryResponse> rows = categoryRepository
                .findByStatusOrderBySortOrderAscUpdatedAtDesc(CATEGORY_STATUS_ENABLED)
                .stream()
                .map(this::toCategoryResponse)
                .toList();
        return Result.success(rows);
    }

    @Transactional
    public Result<List<OpsDiscoverCategoryResponse>> createCategory(OpsDiscoverCategoryRequest request) {
        String normalizedName = normalizeCategoryName(request == null ? null : request.name());
        if (normalizedName == null) {
            return Result.error("Category name is required");
        }
        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            return Result.error("Category name already exists");
        }
        OpsDiscoverCategory row = new OpsDiscoverCategory();
        row.setName(normalizedName);
        row.setSortOrder(categorySortOrder(request == null ? null : request.sortOrder()));
        row.setStatus(CATEGORY_STATUS_ENABLED);
        categoryRepository.save(row);
        return listCategories();
    }

    @Transactional
    public Result<List<OpsDiscoverCategoryResponse>> updateCategory(Long id, OpsDiscoverCategoryRequest request) {
        if (id == null) {
            return Result.error("Category id is required");
        }
        OpsDiscoverCategory row = categoryRepository.findById(id).orElse(null);
        if (row == null || !CATEGORY_STATUS_ENABLED.equals(row.getStatus())) {
            return Result.error("Category not found");
        }
        String normalizedName = normalizeCategoryName(request == null ? null : request.name());
        if (normalizedName == null) {
            return Result.error("Category name is required");
        }
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, row.getId())) {
            return Result.error("Category name already exists");
        }
        String oldName = row.getName();
        row.setName(normalizedName);
        row.setSortOrder(categorySortOrder(request == null ? null : request.sortOrder()));
        categoryRepository.save(row);
        if (!oldName.equals(normalizedName)) {
            OpsContentSlot heroSlot = ensureSlot(SLOT_DISCOVER_HERO, "Discover Hero", "DISCOVER", "HERO");
            OpsContentSlot bannerSlot =
                    ensureSlot(SLOT_LIBRARY_COLDSTART_HERO, "Library Coldstart Hero", "LIBRARY", "TOP_BANNER");
            OpsContentSlot communitySlot = ensureSlot(SLOT_COMMUNITY_TODAY, "Community Today", "COMMUNITY", "TODAY");
            itemRepository.updateBadgeTextBySlotIdAndBadgeText(heroSlot.getId(), oldName, normalizedName);
            itemRepository.updateBadgeTextBySlotIdAndBadgeText(bannerSlot.getId(), oldName, normalizedName);
            itemRepository.updateBadgeTextBySlotIdAndBadgeText(communitySlot.getId(), oldName, normalizedName);
        }
        return listCategories();
    }

    @Transactional
    public Result<List<OpsDiscoverCategoryResponse>> deleteCategory(Long id) {
        if (id == null) {
            return Result.error("Category id is required");
        }
        OpsDiscoverCategory row = categoryRepository.findById(id).orElse(null);
        if (row == null || !CATEGORY_STATUS_ENABLED.equals(row.getStatus())) {
            return Result.error("Category not found");
        }
        OpsContentSlot communitySlot = ensureSlot(SLOT_COMMUNITY_TODAY, "Community Today", "COMMUNITY", "TODAY");
        OpsContentSlot heroSlot = ensureSlot(SLOT_DISCOVER_HERO, "Discover Hero", "DISCOVER", "HERO");
        OpsContentSlot bannerSlot =
                ensureSlot(SLOT_LIBRARY_COLDSTART_HERO, "Library Coldstart Hero", "LIBRARY", "TOP_BANNER");
        long usedCount = itemRepository.countBySlotIdAndBadgeText(communitySlot.getId(), row.getName())
                + itemRepository.countBySlotIdAndBadgeText(heroSlot.getId(), row.getName())
                + itemRepository.countBySlotIdAndBadgeText(bannerSlot.getId(), row.getName());
        if (usedCount > 0) {
            return Result.error("Category is used by placements and cannot be deleted");
        }
        categoryRepository.delete(row);
        return listCategories();
    }

    private String saveCommunityItems(
            Long slotId,
            List<OpsDiscoverConfigUpdateRequest.CommunityItemConfig> items,
            Map<String, Game> gameByAppId,
            Set<String> validCategoryNames
    ) {
        if (items == null) {
            return null;
        }
        Set<String> dedupe = new HashSet<>();
        int sort = 0;
        for (OpsDiscoverConfigUpdateRequest.CommunityItemConfig cfg : items) {
            if (cfg == null || cfg.appId() == null || cfg.appId().isBlank()) {
                continue;
            }
            String dedupeKey = cfg.appId().trim() + "|" + normalizeCategoryName(cfg.cardCategory()) + "|" + (cfg.articleTitle() == null ? "" : cfg.articleTitle().trim());
            if (!dedupe.add(dedupeKey)) {
                return "Community item duplicated for appId/category/title";
            }
            Game game = gameByAppId.get(cfg.appId());
            if (game == null) {
                continue;
            }
            String categoryName = normalizeCategoryName(cfg.cardCategory());
            if (categoryName == null) {
                return "Community item category is required";
            }
            if (!validCategoryNames.contains(categoryName)) {
                return "Community item category does not exist: " + categoryName;
            }
            String status = normalizePlacementStatus(cfg.status());
            if (status == null) {
                return "Community item status is invalid";
            }
            if (!isValidSchedule(cfg.startAt(), cfg.endAt())) {
                return "Community item schedule is invalid";
            }
            if (cfg.cardTitle() != null && cfg.cardTitle().length() > 128) {
                return "Community card title is too long";
            }
            if (cfg.articleTitle() != null && cfg.articleTitle().length() > 256) {
                return "Community article title is too long";
            }
            if (cfg.actionText() != null && cfg.actionText().length() > 64) {
                return "Community action text is too long";
            }
            if (!isValidUrl(cfg.coverUrl())) {
                return "Community cover url is invalid";
            }
            OpsContentItem item = new OpsContentItem();
            item.setSlotId(slotId);
            item.setGameId(game.getId());
            item.setBadgeText(categoryName);
            item.setTitle(cfg.cardTitle());
            item.setCoverUrl(cfg.coverUrl());
            item.setArticleTag(cfg.articleTag());
            item.setArticleTitle(cfg.articleTitle());
            item.setArticleBody(cfg.articleBody());
            item.setActionText(cfg.actionText());
            item.setStatus(status);
            item.setStartAt(cfg.startAt());
            item.setEndAt(cfg.endAt());
            item.setSortOrder(cfg.sortOrder() == null ? sort : cfg.sortOrder());
            itemRepository.save(item);
            sort++;
        }
        return null;
    }

    private String saveTopBannerItems(
            Long slotId,
            List<OpsDiscoverConfigUpdateRequest.TopBannerConfig> items,
            Map<String, Game> gameByAppId,
            Set<String> validCategoryNames
    ) {
        if (items == null) {
            return null;
        }
        Set<String> uniqueAppIds = new HashSet<>();
        int sort = 0;
        for (OpsDiscoverConfigUpdateRequest.TopBannerConfig cfg : items) {
            if (cfg == null || cfg.appId() == null || cfg.appId().isBlank()) {
                continue;
            }
            if (!uniqueAppIds.add(cfg.appId().trim())) {
                return "Top banner game appId duplicated: " + cfg.appId();
            }
            Game game = gameByAppId.get(cfg.appId());
            if (game == null) {
                return "Top banner game appId is invalid: " + cfg.appId();
            }
            String badgeText = normalizeCategoryName(cfg.badgeText());
            if (badgeText != null && !validCategoryNames.contains(badgeText)) {
                return "Top banner category does not exist: " + badgeText;
            }
            String status = normalizePlacementStatus(cfg.status());
            if (status == null) {
                return "Top banner status is invalid";
            }
            if (!isValidSchedule(cfg.startAt(), cfg.endAt())) {
                return "Top banner schedule is invalid";
            }
            if (cfg.title() != null && cfg.title().length() > 128) {
                return "Top banner title is too long";
            }
            if (cfg.subtitle() != null && cfg.subtitle().length() > 256) {
                return "Top banner subtitle is too long";
            }
            if (!isValidUrl(cfg.coverUrl())) {
                return "Top banner cover url is invalid";
            }
            OpsContentItem item = new OpsContentItem();
            item.setSlotId(slotId);
            item.setGameId(game.getId());
            item.setTitle(cfg.title());
            item.setSubtitle(cfg.subtitle());
            item.setBadgeText(badgeText);
            item.setCoverUrl(cfg.coverUrl());
            item.setStatus(status);
            item.setStartAt(cfg.startAt());
            item.setEndAt(cfg.endAt());
            item.setSortOrder(cfg.sortOrder() == null ? sort : cfg.sortOrder());
            itemRepository.save(item);
            sort++;
        }
        return null;
    }

    private void saveSlotItems(Long slotId, List<String> appIds, Map<String, Game> gameByAppId) {
        if (appIds == null) {
            return;
        }
        int sort = 0;
        for (String appId : appIds) {
            if (appId == null || appId.isBlank()) {
                continue;
            }
            Game game = gameByAppId.get(appId);
            if (game == null) {
                continue;
            }
            OpsContentItem item = new OpsContentItem();
            item.setSlotId(slotId);
            item.setGameId(game.getId());
            item.setStatus("PUBLISHED");
            item.setSortOrder(sort++);
            itemRepository.save(item);
        }
    }

    private void replaceCollectionRelations(
            OpsCollection collection,
            List<String> appIds,
            Map<String, Game> gameByAppId,
            User currentUser
    ) {
        collectionGameRelRepository.deleteByCollectionId(collection.getId());
        collection.setStatus("PUBLISHED");
        collection.setUpdatedBy(currentUser == null ? null : currentUser.getId());
        collectionRepository.save(collection);

        if (appIds == null) {
            return;
        }
        int sort = 0;
        List<OpsCollectionGameRel> rows = new ArrayList<>();
        for (String appId : appIds) {
            if (appId == null || appId.isBlank()) {
                continue;
            }
            Game game = gameByAppId.get(appId);
            if (game == null) {
                continue;
            }
            OpsCollectionGameRel row = new OpsCollectionGameRel();
            row.setCollectionId(collection.getId());
            row.setGameId(game.getId());
            row.setPinTop(false);
            row.setSortOrder(sort++);
            rows.add(row);
        }
        if (!rows.isEmpty()) {
            collectionGameRelRepository.saveAll(rows);
        }
    }

    private OpsContentSlot ensureSlot(String slotCode, String name, String pageCode, String positionCode) {
        return slotRepository.findBySlotCode(slotCode).orElseGet(() -> {
            OpsContentSlot slot = new OpsContentSlot();
            slot.setSlotCode(slotCode);
            slot.setName(name);
            slot.setPageCode(pageCode);
            slot.setPositionCode(positionCode);
            slot.setEnabled(true);
            return slotRepository.save(slot);
        });
    }

    private OpsCollection ensureCollection(String code, String name) {
        return collectionRepository.findByCollectionCode(code).orElseGet(() -> {
            OpsCollection collection = new OpsCollection();
            collection.setCollectionCode(code);
            collection.setName(name);
            collection.setPageCode("DISCOVER");
            collection.setStatus("PUBLISHED");
            collection.setStartAt(LocalDateTime.now().minusYears(1));
            return collectionRepository.save(collection);
        });
    }

    private void seedDefaultCommunityItems(Long slotId, List<Game> approvedGames) {
        int sort = 0;
        for (Game game : approvedGames.stream().limit(3).toList()) {
            OpsContentItem item = new OpsContentItem();
            item.setSlotId(slotId);
            item.setGameId(game.getId());
            item.setStatus("PUBLISHED");
            item.setSortOrder(sort++);
            item.setBadgeText(DEFAULT_COMMUNITY_CATEGORY_NAME);
            item.setTitle(game.getName());
            item.setCoverUrl(game.getIconUrl());
            item.setArticleTag("深度測評");
            item.setArticleTitle(game.getName());
            item.setArticleBody(
                    game.getDescription() == null || game.getDescription().isBlank()
                            ? "立即秒開，體驗這款熱門遊戲。"
                            : game.getDescription()
            );
            item.setActionText("立即秒開");
            itemRepository.save(item);
        }
    }

    private void ensureDefaultCategorySeed() {
        if (!categoryRepository.existsByNameIgnoreCase(DEFAULT_COMMUNITY_CATEGORY_NAME)) {
            OpsDiscoverCategory row = new OpsDiscoverCategory();
            row.setName(DEFAULT_COMMUNITY_CATEGORY_NAME);
            row.setSortOrder(0);
            row.setStatus(CATEGORY_STATUS_ENABLED);
            categoryRepository.save(row);
        }
    }

    private List<OpsDiscoverConfigResponse.CategoryOption> buildCategoryOptions() {
        return categoryRepository
                .findByStatusOrderBySortOrderAscUpdatedAtDesc(CATEGORY_STATUS_ENABLED)
                .stream()
                .map(row -> new OpsDiscoverConfigResponse.CategoryOption(row.getId(), row.getName(), row.getSortOrder()))
                .toList();
    }

    private List<OpsDiscoverConfigResponse.PublishOrderSummary> listRecentPublishOrders() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Long> liveOrderByScope = List.of(SCOPE_DISCOVER_BANNERS, SCOPE_GAME_BANNERS, SCOPE_COMMUNITY).stream()
                .map(scope -> findEffectivePublishOrder(scope, now).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(OpsDiscoverPublishOrder::getScopeCode, OpsDiscoverPublishOrder::getId, (left, right) -> left));
        return publishOrderRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(order -> new OpsDiscoverConfigResponse.PublishOrderSummary(
                        order.getId(),
                        order.getOrderNo(),
                        order.getScopeCode(),
                        order.getScopeName(),
                        order.getStatus(),
                        order.getEffectiveAt(),
                        order.getReason(),
                        order.getCreatedByName(),
                        order.getCreatedAt(),
                        Objects.equals(liveOrderByScope.get(order.getScopeCode()), order.getId())
                ))
                .toList();
    }

    private List<OpsDiscoverConfigResponse.ExperimentConfig> listRecentExperiments() {
        return experimentRepository.findTop20ByOrderByUpdatedAtDesc().stream()
                .map(experiment -> new OpsDiscoverConfigResponse.ExperimentConfig(
                        experiment.getId(),
                        experiment.getSlotCode(),
                        experiment.getScopeCode(),
                        experiment.getExperimentName(),
                        experiment.getTrafficPercent(),
                        experiment.getStatus(),
                        experiment.getStartAt(),
                        experiment.getEndAt(),
                        experiment.getNote(),
                        countVariantItems(experiment.getScopeCode(), experiment.getVariantPayloadJson())
                ))
                .toList();
    }

    private OpsDiscoverConfigResponse.ScopePreview buildScopePreview(
            String scopeCode,
            List<?> draftItems,
            List<?> liveItems
    ) {
        OpsDiscoverPublishPreviewResponse preview = buildPublishPreview(scopeCode, draftItems, liveItems);
        return new OpsDiscoverConfigResponse.ScopePreview(
                scopeCode,
                scopeLabel(scopeCode),
                preview.draftCount(),
                preview.liveCount(),
                preview.addedCount(),
                preview.removedCount(),
                preview.changedCount(),
                preview.nextScheduledAt(),
                preview.addedCount() > 0 || preview.removedCount() > 0 || preview.changedCount() > 0
        );
    }

    private OpsDiscoverPublishPreviewResponse buildPublishPreview(String scopeCode) {
        return switch (scopeCode) {
            case SCOPE_DISCOVER_BANNERS -> buildPublishPreview(scopeCode, buildDraftTopBannerScope(SCOPE_DISCOVER_BANNERS), resolveLiveTopBannerScope(SCOPE_DISCOVER_BANNERS));
            case SCOPE_GAME_BANNERS -> buildPublishPreview(scopeCode, buildDraftTopBannerScope(SCOPE_GAME_BANNERS), resolveLiveTopBannerScope(SCOPE_GAME_BANNERS));
            case SCOPE_COMMUNITY -> buildPublishPreview(scopeCode, buildDraftCommunityScope(), resolveLiveCommunityScope());
            default -> new OpsDiscoverPublishPreviewResponse(scopeCode, scopeLabel(scopeCode), 0, 0, 0, 0, 0, null, List.of(), List.of("Unsupported scope"));
        };
    }

    private OpsDiscoverPublishPreviewResponse buildPublishPreview(String scopeCode, List<?> draftItems, List<?> liveItems) {
        Map<String, Map<String, Object>> draftMap = indexScopeItems(scopeCode, draftItems);
        Map<String, Map<String, Object>> liveMap = indexScopeItems(scopeCode, liveItems);
        List<OpsDiscoverPublishPreviewResponse.DiffItem> changes = new ArrayList<>();
        int added = 0;
        int removed = 0;
        int changed = 0;

        for (Map.Entry<String, Map<String, Object>> entry : draftMap.entrySet()) {
            Map<String, Object> live = liveMap.remove(entry.getKey());
            if (live == null) {
                added++;
                changes.add(new OpsDiscoverPublishPreviewResponse.DiffItem("ADDED", stringValue(entry.getValue().get("appId")), stringValue(entry.getValue().get("title")), "Draft item is new"));
                continue;
            }
            if (!entry.getValue().equals(live)) {
                changed++;
                changes.add(new OpsDiscoverPublishPreviewResponse.DiffItem("CHANGED", stringValue(entry.getValue().get("appId")), stringValue(entry.getValue().get("title")), "Draft differs from live content"));
            }
        }
        for (Map<String, Object> orphan : liveMap.values()) {
            removed++;
            changes.add(new OpsDiscoverPublishPreviewResponse.DiffItem("REMOVED", stringValue(orphan.get("appId")), stringValue(orphan.get("title")), "Live item will be removed"));
        }

        List<String> warnings = new ArrayList<>();
        if (draftItems.isEmpty()) {
            warnings.add("Current draft scope is empty");
        }
        if (added == 0 && removed == 0 && changed == 0) {
            warnings.add("Draft and live content are identical");
        }

        return new OpsDiscoverPublishPreviewResponse(
                scopeCode,
                scopeLabel(scopeCode),
                draftItems.size(),
                liveItems.size(),
                added,
                removed,
                changed,
                findNextScheduledAt(scopeCode),
                changes.stream().limit(20).toList(),
                warnings
        );
    }

    private Map<String, Map<String, Object>> indexScopeItems(String scopeCode, List<?> items) {
        Map<String, Map<String, Object>> indexed = new LinkedHashMap<>();
        int position = 0;
        for (Object item : items) {
            Map<String, Object> row = switch (scopeCode) {
                case SCOPE_DISCOVER_BANNERS, SCOPE_GAME_BANNERS -> indexTopBannerItem((OpsDiscoverConfigResponse.TopBannerConfig) item, position);
                case SCOPE_COMMUNITY -> indexCommunityItem((OpsDiscoverConfigResponse.CommunityItemConfig) item, position);
                default -> Map.of();
            };
            indexed.put(stringValue(row.get("key")), row);
            position++;
        }
        return indexed;
    }

    private Map<String, Object> indexTopBannerItem(OpsDiscoverConfigResponse.TopBannerConfig item, int position) {
        return Map.of(
                "key", stringValue(item.appId()) + "#" + position,
                "appId", stringValue(item.appId()),
                "title", stringValue(item.title()),
                "subtitle", stringValue(item.subtitle()),
                "badgeText", stringValue(item.badgeText()),
                "coverUrl", stringValue(item.coverUrl()),
                "status", stringValue(item.status()),
                "startAt", String.valueOf(item.startAt()),
                "endAt", String.valueOf(item.endAt())
        );
    }

    private Map<String, Object> indexCommunityItem(OpsDiscoverConfigResponse.CommunityItemConfig item, int position) {
        return Map.of(
                "key", stringValue(item.appId()) + "#" + stringValue(item.cardCategory()) + "#" + position,
                "appId", stringValue(item.appId()),
                "title", stringValue(item.cardTitle()),
                "cardCategory", stringValue(item.cardCategory()),
                "articleTitle", stringValue(item.articleTitle()),
                "coverUrl", stringValue(item.coverUrl()),
                "status", stringValue(item.status()),
                "startAt", String.valueOf(item.startAt()),
                "endAt", String.valueOf(item.endAt())
        );
    }

    private LocalDateTime findNextScheduledAt(String scopeCode) {
        return publishOrderRepository
                .findTopByScopeCodeAndStatusAndEffectiveAtAfterOrderByEffectiveAtAscCreatedAtAsc(scopeCode, ORDER_STATUS_SCHEDULED, LocalDateTime.now())
                .map(OpsDiscoverPublishOrder::getEffectiveAt)
                .orElse(null);
    }

    private List<OpsDiscoverConfigResponse.TopBannerConfig> buildDraftTopBannerScope(String scopeCode) {
        OpsContentSlot slot = resolveScopeSlot(scopeCode);
        if (slot == null) {
            return List.of();
        }
        Map<Long, Game> gameById = gameRepository.findByStatusOrderByCreatedAtDesc(Game.GameStatus.APPROVED)
                .stream()
                .collect(Collectors.toMap(Game::getId, Function.identity(), (left, right) -> left));
        return itemRepository.findBySlotIdOrderBySortOrderAscUpdatedAtDesc(slot.getId()).stream()
                .map(item -> toTopBannerConfig(item, gameById.get(item.getGameId())))
                .toList();
    }

    private List<OpsDiscoverConfigResponse.TopBannerConfig> resolveLiveTopBannerScope(String scopeCode) {
        return findEffectivePublishOrder(scopeCode, LocalDateTime.now())
                .map(order -> readTopBannerPayload(order.getPayloadJson()))
                .orElseGet(() -> {
                    OpsContentSlot slot = resolveScopeSlot(scopeCode);
                    if (slot == null) {
                        return List.of();
                    }
                    Map<Long, Game> gameById = gameRepository.findByStatusOrderByCreatedAtDesc(Game.GameStatus.APPROVED)
                            .stream()
                            .collect(Collectors.toMap(Game::getId, Function.identity(), (left, right) -> left));
                    return itemRepository.findBySlotIdOrderBySortOrderAscUpdatedAtDesc(slot.getId()).stream()
                            .filter(item -> "PUBLISHED".equals(item.getStatus()))
                            .map(item -> toTopBannerConfig(item, gameById.get(item.getGameId())))
                            .toList();
                });
    }

    private List<OpsDiscoverConfigResponse.CommunityItemConfig> buildDraftCommunityScope() {
        OpsContentSlot slot = resolveScopeSlot(SCOPE_COMMUNITY);
        if (slot == null) {
            return List.of();
        }
        Map<Long, Game> gameById = gameRepository.findByStatusOrderByCreatedAtDesc(Game.GameStatus.APPROVED)
                .stream()
                .collect(Collectors.toMap(Game::getId, Function.identity(), (left, right) -> left));
        return itemRepository.findBySlotIdOrderBySortOrderAscUpdatedAtDesc(slot.getId()).stream()
                .map(item -> toCommunityConfig(item, gameById.get(item.getGameId())))
                .toList();
    }

    private List<OpsDiscoverConfigResponse.CommunityItemConfig> resolveLiveCommunityScope() {
        return findEffectivePublishOrder(SCOPE_COMMUNITY, LocalDateTime.now())
                .map(order -> readCommunityPayload(order.getPayloadJson()))
                .orElseGet(() -> {
                    OpsContentSlot slot = resolveScopeSlot(SCOPE_COMMUNITY);
                    if (slot == null) {
                        return List.of();
                    }
                    Map<Long, Game> gameById = gameRepository.findByStatusOrderByCreatedAtDesc(Game.GameStatus.APPROVED)
                            .stream()
                            .collect(Collectors.toMap(Game::getId, Function.identity(), (left, right) -> left));
                    return itemRepository.findBySlotIdOrderBySortOrderAscUpdatedAtDesc(slot.getId()).stream()
                            .filter(item -> "PUBLISHED".equals(item.getStatus()))
                            .map(item -> toCommunityConfig(item, gameById.get(item.getGameId())))
                            .toList();
                });
    }

    private Optional<OpsDiscoverPublishOrder> findEffectivePublishOrder(String scopeCode, LocalDateTime now) {
        return publishOrderRepository
                .findTopByScopeCodeAndStatusInAndEffectiveAtLessThanEqualOrderByEffectiveAtDescCreatedAtDesc(
                        scopeCode,
                        List.of(ORDER_STATUS_PUBLISHED, ORDER_STATUS_SCHEDULED),
                        now
                );
    }

    private String serializeScopePayload(String scopeCode) {
        try {
            return switch (scopeCode) {
                case SCOPE_DISCOVER_BANNERS, SCOPE_GAME_BANNERS -> objectMapper.writeValueAsString(buildDraftTopBannerScope(scopeCode));
                case SCOPE_COMMUNITY -> objectMapper.writeValueAsString(buildDraftCommunityScope());
                default -> "[]";
            };
        } catch (Exception error) {
            throw new IllegalStateException("Failed to serialize scope payload", error);
        }
    }

    private List<OpsDiscoverConfigResponse.TopBannerConfig> readTopBannerPayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<List<OpsDiscoverConfigResponse.TopBannerConfig>>() {});
        } catch (Exception error) {
            return List.of();
        }
    }

    private List<OpsDiscoverConfigResponse.CommunityItemConfig> readCommunityPayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<List<OpsDiscoverConfigResponse.CommunityItemConfig>>() {});
        } catch (Exception error) {
            return List.of();
        }
    }

    private Integer countVariantItems(String scopeCode, String payloadJson) {
        return switch (scopeCode) {
            case SCOPE_DISCOVER_BANNERS, SCOPE_GAME_BANNERS -> readTopBannerPayload(payloadJson).size();
            case SCOPE_COMMUNITY -> readCommunityPayload(payloadJson).size();
            default -> 0;
        };
    }

    private OpsDiscoverAnalyticsDto.ScopeMetrics buildScopeAnalytics(String scopeCode, List<?> draftItems) {
        LocalDateTime now = LocalDateTime.now();
        List<String> draftStates = draftItems.stream().map(this::extractDiscoverItemStatus).toList();
        List<OpsDiscoverPublishOrder> orders = publishOrderRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .filter(order -> scopeCode.equals(order.getScopeCode()))
                .toList();
        List<OpsDiscoverExperiment> experiments = experimentRepository.findTop20ByOrderByUpdatedAtDesc().stream()
                .filter(experiment -> scopeCode.equals(experiment.getScopeCode()))
                .toList();
        return new OpsDiscoverAnalyticsDto.ScopeMetrics(
                scopeCode,
                scopeLabel(scopeCode),
                draftItems.size(),
                countState(draftStates, "LIVE"),
                countState(draftStates, "SCHEDULED"),
                countState(draftStates, "PAUSED"),
                countState(draftStates, "EXPIRED"),
                (int) orders.stream().filter(order -> ORDER_STATUS_SCHEDULED.equals(order.getStatus()) && order.getEffectiveAt() != null && order.getEffectiveAt().isAfter(now)).count(),
                (int) experiments.stream().filter(experiment -> EXPERIMENT_STATUS_ACTIVE.equals(experiment.getStatus())).count(),
                (int) experiments.stream().filter(experiment -> EXPERIMENT_STATUS_ACTIVE.equals(experiment.getStatus()) && experiment.getStartAt() != null && experiment.getStartAt().isAfter(now)).count(),
                experiments.stream()
                        .filter(experiment -> EXPERIMENT_STATUS_ACTIVE.equals(experiment.getStatus()) || EXPERIMENT_STATUS_DRAFT.equals(experiment.getStatus()))
                        .map(OpsDiscoverExperiment::getTrafficPercent)
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .sum(),
                orders.stream()
                        .filter(order -> ORDER_STATUS_SCHEDULED.equals(order.getStatus()))
                        .map(OpsDiscoverPublishOrder::getEffectiveAt)
                        .filter(Objects::nonNull)
                        .min(LocalDateTime::compareTo)
                        .orElse(null)
        );
    }

    private String extractDiscoverItemStatus(Object item) {
        if (item instanceof OpsDiscoverConfigResponse.TopBannerConfig banner) {
            return derivePlacementStatus(banner.status(), banner.startAt(), banner.endAt());
        }
        if (item instanceof OpsDiscoverConfigResponse.CommunityItemConfig communityItem) {
            return derivePlacementStatus(communityItem.status(), communityItem.startAt(), communityItem.endAt());
        }
        return "DRAFT";
    }

    private String derivePlacementStatus(String status, LocalDateTime startAt, LocalDateTime endAt) {
        String normalized = status == null ? "DRAFT" : status.toUpperCase();
        if (!"PUBLISHED".equals(normalized)) {
            return normalized;
        }
        LocalDateTime now = LocalDateTime.now();
        if (startAt != null && startAt.isAfter(now)) {
            return "SCHEDULED";
        }
        if (endAt != null && endAt.isBefore(now)) {
            return "EXPIRED";
        }
        return "LIVE";
    }

    private int countState(List<String> states, String target) {
        return (int) states.stream().filter(target::equals).count();
    }

    private String normalizeBatchTarget(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return Set.of("PUBLISH_ORDER", "EXPERIMENT").contains(normalized) ? normalized : null;
    }

    private OpsContentSlot resolveScopeSlot(String scopeCode) {
        return switch (scopeCode) {
            case SCOPE_DISCOVER_BANNERS -> ensureSlot(SLOT_DISCOVER_HERO, "Discover Hero", "DISCOVER", "HERO");
            case SCOPE_GAME_BANNERS -> ensureSlot(SLOT_LIBRARY_COLDSTART_HERO, "Library Coldstart Hero", "LIBRARY", "TOP_BANNER");
            case SCOPE_COMMUNITY -> ensureSlot(SLOT_COMMUNITY_TODAY, "Community Today", "COMMUNITY", "TODAY");
            default -> null;
        };
    }

    private String scopeSlotCode(String scopeCode) {
        return switch (scopeCode) {
            case SCOPE_DISCOVER_BANNERS -> SLOT_DISCOVER_HERO;
            case SCOPE_GAME_BANNERS -> SLOT_LIBRARY_COLDSTART_HERO;
            case SCOPE_COMMUNITY -> SLOT_COMMUNITY_TODAY;
            default -> "";
        };
    }

    private String scopeLabel(String scopeCode) {
        return switch (scopeCode) {
            case SCOPE_DISCOVER_BANNERS -> "Discover Banners";
            case SCOPE_GAME_BANNERS -> "Game Banners";
            case SCOPE_COMMUNITY -> "Community Recommendations";
            default -> scopeCode;
        };
    }

    private String normalizeScopeCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        return Set.of(SCOPE_DISCOVER_BANNERS, SCOPE_GAME_BANNERS, SCOPE_COMMUNITY).contains(normalized) ? normalized : null;
    }

    private String normalizeOrderStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        return Set.of(ORDER_STATUS_PUBLISHED, ORDER_STATUS_SCHEDULED, ORDER_STATUS_CANCELLED).contains(normalized) ? normalized : null;
    }

    private String normalizeExperimentStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return EXPERIMENT_STATUS_DRAFT;
        }
        String normalized = raw.trim().toUpperCase();
        return Set.of(EXPERIMENT_STATUS_DRAFT, EXPERIMENT_STATUS_ACTIVE, EXPERIMENT_STATUS_PAUSED).contains(normalized) ? normalized : null;
    }

    private OpsDiscoverConfigResponse.TopBannerConfig toTopBannerConfig(OpsContentItem item, Game game) {
        return new OpsDiscoverConfigResponse.TopBannerConfig(
                item.getId(),
                game == null ? "" : game.getAppId(),
                item.getTitle(),
                item.getSubtitle(),
                item.getBadgeText(),
                item.getCoverUrl(),
                item.getStatus(),
                item.getStartAt(),
                item.getEndAt(),
                item.getSortOrder()
        );
    }

    private OpsDiscoverConfigResponse.CommunityItemConfig toCommunityConfig(OpsContentItem item, Game game) {
        return new OpsDiscoverConfigResponse.CommunityItemConfig(
                item.getId(),
                game == null ? "" : game.getAppId(),
                item.getBadgeText(),
                item.getTitle(),
                item.getCoverUrl(),
                item.getArticleTag(),
                item.getArticleTitle(),
                item.getArticleBody(),
                item.getActionText(),
                item.getStatus(),
                item.getStartAt(),
                item.getEndAt(),
                item.getSortOrder()
        );
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private List<OpsDiscoverConfigResponse.SlotControl> buildSlotControls(List<OpsContentSlot> slots) {
        return slots.stream()
                .map(slot -> new OpsDiscoverConfigResponse.SlotControl(
                        slot.getSlotCode(),
                        slot.getName(),
                        slot.getPageCode(),
                        slot.getPositionCode(),
                        Boolean.TRUE.equals(slot.getEnabled())
                ))
                .toList();
    }

    private OpsDiscoverCategoryResponse toCategoryResponse(OpsDiscoverCategory row) {
        OpsContentSlot heroSlot = ensureSlot(SLOT_DISCOVER_HERO, "Discover Hero", "DISCOVER", "HERO");
        OpsContentSlot bannerSlot =
                ensureSlot(SLOT_LIBRARY_COLDSTART_HERO, "Library Coldstart Hero", "LIBRARY", "TOP_BANNER");
        OpsContentSlot communitySlot = ensureSlot(SLOT_COMMUNITY_TODAY, "Community Today", "COMMUNITY", "TODAY");
        long bannerUsageCount = itemRepository.countBySlotIdAndBadgeText(heroSlot.getId(), row.getName())
                + itemRepository.countBySlotIdAndBadgeText(bannerSlot.getId(), row.getName());
        long recommendationUsageCount = itemRepository.countBySlotIdAndBadgeText(communitySlot.getId(), row.getName());
        return new OpsDiscoverCategoryResponse(
                row.getId(),
                row.getName(),
                row.getSortOrder(),
                bannerUsageCount + recommendationUsageCount,
                bannerUsageCount,
                recommendationUsageCount
        );
    }

    private String normalizePlacementStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return "DRAFT";
        }
        String normalized = raw.trim().toUpperCase();
        return switch (normalized) {
            case "DRAFT", "PUBLISHED", "PAUSED" -> normalized;
            default -> null;
        };
    }

    private boolean isValidSchedule(LocalDateTime startAt, LocalDateTime endAt) {
        return startAt == null || endAt == null || !endAt.isBefore(startAt);
    }

    private boolean isValidUrl(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim();
        return normalized.startsWith("http://")
                || normalized.startsWith("https://")
                || normalized.startsWith("/");
    }

    private String validateContentItemForStatusChange(
            OpsContentItem item,
            String targetStatus,
            Map<Long, Game> approvedGameById,
            Set<String> validCategoryNames
    ) {
        if (!Set.of("DRAFT", "PUBLISHED", "PAUSED").contains(targetStatus)) {
            return "Unsupported target status";
        }
        if ("PUBLISHED".equals(targetStatus)) {
            Game game = approvedGameById.get(item.getGameId());
            if (game == null) {
                return "Only approved games can be published";
            }
            if (!isValidSchedule(item.getStartAt(), item.getEndAt())) {
                return "Item schedule is invalid";
            }
            if (item.getBadgeText() != null && !item.getBadgeText().isBlank()
                    && !validCategoryNames.contains(normalizeCategoryName(item.getBadgeText()))) {
                return "Item category does not exist";
            }
            if (!isValidUrl(item.getCoverUrl())) {
                return "Item cover url is invalid";
            }
        }
        return null;
    }

    private Integer categorySortOrder(Integer inputSortOrder) {
        return inputSortOrder == null ? 0 : Math.max(0, inputSortOrder);
    }

    private boolean isManagedSlotCode(String slotCode) {
        return Set.of(
                SLOT_DISCOVER_HERO,
                SLOT_LIBRARY_COLDSTART_HERO,
                SLOT_DISCOVER_RANK,
                SLOT_COMMUNITY_TODAY
        ).contains(slotCode);
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
