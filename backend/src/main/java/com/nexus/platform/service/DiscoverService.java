package com.nexus.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.dto.DiscoverFeedItem;
import com.nexus.platform.dto.DiscoverCommunityItem;
import com.nexus.platform.dto.DiscoverHeroCard;
import com.nexus.platform.dto.DiscoverHomeResponse;
import com.nexus.platform.dto.OpsDiscoverConfigResponse;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.GameOpsProfile;
import com.nexus.platform.entity.GameVersion;
import com.nexus.platform.entity.OpsCollection;
import com.nexus.platform.entity.OpsCollectionGameRel;
import com.nexus.platform.entity.OpsContentItem;
import com.nexus.platform.entity.OpsContentSlot;
import com.nexus.platform.entity.OpsDiscoverExperiment;
import com.nexus.platform.entity.OpsDiscoverPublishOrder;
import com.nexus.platform.entity.OpsGameCategory;
import com.nexus.platform.repository.OpsDiscoverExperimentRepository;
import com.nexus.platform.repository.OpsDiscoverPublishOrderRepository;
import com.nexus.platform.repository.GameOpsProfileRepository;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.GameVersionRepository;
import com.nexus.platform.repository.OpsCollectionGameRelRepository;
import com.nexus.platform.repository.OpsCollectionRepository;
import com.nexus.platform.repository.OpsContentItemRepository;
import com.nexus.platform.repository.OpsContentSlotRepository;
import com.nexus.platform.repository.OpsGameCategoryRepository;
import com.nexus.platform.repository.UserGameActionLogRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiscoverService {
    private static final String CATEGORY_STATUS_ENABLED = "ENABLED";
    private static final String SLOT_DISCOVER_HERO = "DISCOVER_HERO";
    private static final String SLOT_LIBRARY_COLDSTART_HERO = "LIBRARY_COLDSTART_HERO";
    private static final String SLOT_DISCOVER_RANK = "DISCOVER_RANK";
    private static final String SLOT_COMMUNITY_TODAY = "COMMUNITY_TODAY";
    private static final String COLLECTION_NEWBIE_MUST_PLAY = "NEWBIE_MUST_PLAY";
    private static final String SCOPE_DISCOVER_BANNERS = "DISCOVER_BANNERS";
    private static final String SCOPE_GAME_BANNERS = "GAME_BANNERS";
    private static final String SCOPE_COMMUNITY = "COMMUNITY";
    private static final String ORDER_STATUS_PUBLISHED = "PUBLISHED";
    private static final String ORDER_STATUS_SCHEDULED = "SCHEDULED";
    private static final String EXPERIMENT_STATUS_ACTIVE = "ACTIVE";

    private final GameRepository gameRepository;
    private final GameOpsProfileRepository gameOpsProfileRepository;
    private final GameVersionRepository gameVersionRepository;
    private final OpsContentSlotRepository opsContentSlotRepository;
    private final OpsContentItemRepository opsContentItemRepository;
    private final OpsGameCategoryRepository gameCategoryRepository;
    private final OpsCollectionRepository opsCollectionRepository;
    private final OpsCollectionGameRelRepository opsCollectionGameRelRepository;
    private final UserGameActionLogRepository actionLogRepository;
    private final OpsDiscoverPublishOrderRepository publishOrderRepository;
    private final OpsDiscoverExperimentRepository experimentRepository;
    private final ObjectMapper objectMapper;
    @Value("${platform.public-base-url}")
    private String publicBaseUrl;

    public Result<List<DiscoverFeedItem>> getFeed(int limit, String category) {
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        String normalizedCategory = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);

        List<Game> rankedGames = resolveSlotGames(SLOT_DISCOVER_RANK, 100);
        if (rankedGames.isEmpty()) {
            rankedGames = getDefaultApprovedGames(100);
        }
        if (!normalizedCategory.isBlank() && !"all".equals(normalizedCategory)) {
            rankedGames = rankedGames.stream()
                    .filter(game -> matchesCategory(game, normalizedCategory))
                    .toList();
        }

        List<DiscoverFeedItem> feed = rankedGames.stream()
                .limit(normalizedLimit)
                .map(this::toDiscoverFeedItemWithOpsProfile)
                .toList();
        return Result.success(feed);
    }

    public Result<DiscoverHomeResponse> getHome(int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        boolean discoverHeroEnabled = isSlotEnabled(SLOT_DISCOVER_HERO);
        boolean libraryTopBannerEnabled = isSlotEnabled(SLOT_LIBRARY_COLDSTART_HERO);
        boolean discoverRankEnabled = isSlotEnabled(SLOT_DISCOVER_RANK);
        DiscoverHeroCard hero = resolveHeroCard(SLOT_DISCOVER_HERO);
        DiscoverHeroCard libraryTopBanner = resolveHeroCard(SLOT_LIBRARY_COLDSTART_HERO);

        List<DiscoverFeedItem> ranked = (discoverRankEnabled ? resolveSlotGames(SLOT_DISCOVER_RANK, normalizedLimit) : List.<Game>of()).stream()
                .map(this::toDiscoverFeedItemWithOpsProfile)
                .toList();
        if (discoverRankEnabled && ranked.isEmpty()) {
            ranked = getDefaultApprovedGames(normalizedLimit).stream()
                    .map(this::toDiscoverFeedItemWithOpsProfile)
                    .toList();
        }

        List<Game> newbieSource = resolveCollectionGames(COLLECTION_NEWBIE_MUST_PLAY, normalizedLimit);
        if (newbieSource.isEmpty()) {
            newbieSource = getDefaultApprovedGames(normalizedLimit);
        }
        List<DiscoverFeedItem> newbie = newbieSource.stream().map(this::toDiscoverFeedItemWithOpsProfile).toList();

        List<DiscoverFeedItem> everyone = resolveEveryonePlayingGames(normalizedLimit).stream()
                .map(this::toDiscoverFeedItemWithOpsProfile)
                .toList();

        if (discoverHeroEnabled && hero == null && !ranked.isEmpty()) {
            DiscoverFeedItem top = ranked.get(0);
            hero = new DiscoverHeroCard(
                    top.appId(),
                    top.name(),
                    top.description(),
                    top.coverUrl(),
                    "Recommended",
                    "OPEN_GAME"
            );
        }
        if (!discoverHeroEnabled) {
            hero = null;
        }
        if (!libraryTopBannerEnabled) {
            libraryTopBanner = null;
        }
        return Result.success(new DiscoverHomeResponse(
                hero,
                libraryTopBanner,
                listDiscoverCategories(),
                ranked,
                newbie,
                everyone
        ));
    }

    public Result<List<DiscoverCommunityItem>> getCommunity(int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 20));
        if (!isSlotEnabled(SLOT_COMMUNITY_TODAY)) {
            return Result.success(List.of());
        }
        List<OpsDiscoverConfigResponse.CommunityItemConfig> items = resolveCommunityItemsForSlot();

        List<DiscoverCommunityItem> result = new ArrayList<>();
        for (OpsDiscoverConfigResponse.CommunityItemConfig item : items.stream().limit(normalizedLimit).toList()) {
            Game game = gameRepository.findByAppId(item.appId());
            if (game == null || game.getStatus() != Game.GameStatus.APPROVED) {
                continue;
            }
            result.add(toCommunityItem(game, item));
            if (result.size() >= normalizedLimit) {
                break;
            }
        }

        if (result.isEmpty()) {
            List<Game> fallbackGames = resolveEveryonePlayingGames(normalizedLimit);
            for (Game game : fallbackGames) {
                OpsDiscoverConfigResponse.CommunityItemConfig pseudo = new OpsDiscoverConfigResponse.CommunityItemConfig(
                        null,
                        game.getAppId(),
                        "編輯精選",
                        game.getName(),
                        resolveDiscoverCoverUrl(game),
                        "深度測評",
                        game.getName(),
                        textOrFallback(game.getDescription(), "立即秒開，體驗這款熱門遊戲。"),
                        "立即秒開",
                        "PUBLISHED",
                        null,
                        null,
                        0
                );
                result.add(toCommunityItem(game, pseudo));
            }
        }
        return Result.success(result.stream().limit(normalizedLimit).toList());
    }

    private DiscoverFeedItem toDiscoverFeedItemWithOpsProfile(Game game) {
        normalizeClientGame(game);
        long hotScore = calculateHotScoreByAge(game);
        String category = detectCategory(game);
        DiscoverFeedItem base = DiscoverFeedItem.from(game, hotScore, category, List.of("Recommended", "Featured"));
        GameOpsProfile profile = gameOpsProfileRepository.findByGameId(game.getId()).orElse(null);
        if (profile == null) {
            return base;
        }
        return base.withVisualOverrides(profile.getDiscoverCardCoverUrl(), profile.getDiscoverCardLogoUrl());
    }

    private boolean matchesCategory(Game game, String category) {
        return detectCategory(game).equalsIgnoreCase(category);
    }

    private String detectCategory(Game game) {
        if (game.getCategory() != null && !game.getCategory().isBlank()) {
            return game.getCategory().trim();
        }
        String text = ((game.getName() == null ? "" : game.getName()) + " "
                + (game.getDescription() == null ? "" : game.getDescription()))
                .toLowerCase(Locale.ROOT);
        if (containsAny(text, "shoot", "action", "battle", "combat")) {
            return "action";
        }
        if (containsAny(text, "casual", "puzzle", "match", "merge")) {
            return "casual";
        }
        if (containsAny(text, "rpg", "role", "adventure", "quest")) {
            return "rpg";
        }
        return "all";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private long calculateHotScoreByAge(Game game) {
        if (game.getCreatedAt() == null) {
            return 50L;
        }
        long hours = Math.max(1L, Duration.between(game.getCreatedAt(), LocalDateTime.now()).toHours());
        return Math.max(1L, 1000L / hours);
    }

    private DiscoverHeroCard resolveHeroCard(String slotCode) {
        OpsContentSlot heroSlot = opsContentSlotRepository.findBySlotCodeAndEnabledTrue(slotCode).orElse(null);
        if (heroSlot == null) {
            return null;
        }
        List<OpsDiscoverConfigResponse.TopBannerConfig> items = resolveTopBannerItemsForSlot(slotCode);
        if (items.isEmpty()) {
            return null;
        }
        OpsDiscoverConfigResponse.TopBannerConfig first = items.get(0);
        Game game = gameRepository.findByAppId(first.appId());
        if (game == null || game.getStatus() != Game.GameStatus.APPROVED) {
            return null;
        }
        String title = textOrFallback(first.title(), game.getName());
        String subtitle = textOrFallback(first.subtitle(), game.getDescription());
        String coverUrl = textOrFallback(first.coverUrl(), resolveDiscoverCoverUrl(game));
        String badge = textOrFallback(first.badgeText(), "Recommended");
        return new DiscoverHeroCard(game.getAppId(), title, subtitle, coverUrl, badge, "OPEN_GAME");
    }

    private boolean isSlotEnabled(String slotCode) {
        return opsContentSlotRepository.findBySlotCode(slotCode)
                .map(OpsContentSlot::getEnabled)
                .orElse(true);
    }

    private List<String> listDiscoverCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("all");
        categories.addAll(gameCategoryRepository.findByStatusOrderBySortOrderAscUpdatedAtDesc(CATEGORY_STATUS_ENABLED)
                .stream()
                .map(OpsGameCategory::getName)
                .toList());
        return categories;
    }

    private List<Game> resolveSlotGames(String slotCode, int limit) {
        OpsContentSlot slot = opsContentSlotRepository.findBySlotCodeAndEnabledTrue(slotCode).orElse(null);
        if (slot == null) {
            return List.of();
        }
        List<OpsContentItem> items = opsContentItemRepository.findActiveBySlotId(slot.getId(), LocalDateTime.now());
        if (items.isEmpty()) {
            return List.of();
        }
        List<Game> games = new ArrayList<>();
        for (OpsContentItem item : items) {
            Game game = gameRepository.findById(item.getGameId()).orElse(null);
            if (game != null && game.getStatus() == Game.GameStatus.APPROVED) {
                games.add(game);
                if (games.size() >= limit) {
                    break;
                }
            }
        }
        return games;
    }

    private List<Game> resolveCollectionGames(String collectionCode, int limit) {
        LocalDateTime now = LocalDateTime.now();
        OpsCollection collection = opsCollectionRepository
                .findByCollectionCodeAndStatus(collectionCode, "PUBLISHED")
                .filter(c -> (c.getStartAt() == null || !c.getStartAt().isAfter(now))
                        && (c.getEndAt() == null || !c.getEndAt().isBefore(now)))
                .orElse(null);
        if (collection == null) {
            return List.of();
        }
        List<OpsCollectionGameRel> rows = opsCollectionGameRelRepository
                .findByCollectionIdOrderByPinTopDescSortOrderAscUpdatedAtDesc(collection.getId());
        List<Game> result = new ArrayList<>();
        for (OpsCollectionGameRel row : rows) {
            Game game = gameRepository.findById(row.getGameId()).orElse(null);
            if (game != null && game.getStatus() == Game.GameStatus.APPROVED) {
                result.add(game);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    private List<OpsContentItem> resolveSlotItems(String slotCode, int limit) {
        if (SLOT_COMMUNITY_TODAY.equals(slotCode)) {
            List<OpsContentItem> fallback = resolveFallbackSlotItems(slotCode);
            return fallback.stream().limit(limit).toList();
        }
        OpsContentSlot slot = opsContentSlotRepository.findBySlotCodeAndEnabledTrue(slotCode).orElse(null);
        if (slot == null) {
            return List.of();
        }
        List<OpsContentItem> items = opsContentItemRepository.findActiveBySlotId(slot.getId(), LocalDateTime.now());
        if (items.isEmpty()) {
            return List.of();
        }
        return items.stream().limit(limit).toList();
    }

    private DiscoverCommunityItem toCommunityItem(Game game, OpsDiscoverConfigResponse.CommunityItemConfig item) {
        normalizeClientGame(game);
        String fallbackTitle = textOrFallback(item.cardTitle(), game.getName());
        return new DiscoverCommunityItem(
                game.getAppId(),
                textOrFallback(game.getName(), ""),
                detectCategory(game),
                resolveCommunityIconUrl(game),
                textOrFallback(item.cardCategory(), "編輯精選"),
                fallbackTitle,
                textOrFallback(item.coverUrl(), resolveDiscoverCoverUrl(game)),
                textOrFallback(item.articleTag(), "深度測評"),
                textOrFallback(item.articleTitle(), fallbackTitle),
                textOrFallback(item.articleBody(), textOrFallback(game.getDescription(), "")),
                textOrFallback(item.actionText(), "立即秒開")
        );
    }

    private List<Game> resolveEveryonePlayingGames(int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<UserGameActionLogRepository.GameActionAggregate> aggregates =
                actionLogRepository.aggregateTopGamesByActionSince("PLAY", since);
        List<Game> result = new ArrayList<>();
        for (UserGameActionLogRepository.GameActionAggregate aggregate : aggregates) {
            if (aggregate.getGameId() == null) {
                continue;
            }
            Game game = gameRepository.findById(aggregate.getGameId()).orElse(null);
            if (game != null && game.getStatus() == Game.GameStatus.APPROVED) {
                result.add(game);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        if (result.isEmpty()) {
            return getDefaultApprovedGames(limit);
        }
        return result;
    }

    private List<Game> getDefaultApprovedGames(int limit) {
        return gameRepository.findByStatusOrderByCreatedAtDesc(Game.GameStatus.APPROVED).stream()
                .limit(limit)
                .toList();
    }

    private List<OpsDiscoverConfigResponse.TopBannerConfig> resolveTopBannerItemsForSlot(String slotCode) {
        String scopeCode = slotToScope(slotCode);
        if (scopeCode == null) {
            return List.of();
        }
        List<OpsDiscoverConfigResponse.TopBannerConfig> experimentVariant = resolveExperimentTopBannerVariant(slotCode);
        if (!experimentVariant.isEmpty()) {
            return experimentVariant;
        }
        Optional<OpsDiscoverPublishOrder> order = publishOrderRepository
                .findTopByScopeCodeAndStatusInAndEffectiveAtLessThanEqualOrderByEffectiveAtDescCreatedAtDesc(
                        scopeCode,
                        List.of(ORDER_STATUS_PUBLISHED, ORDER_STATUS_SCHEDULED),
                        LocalDateTime.now()
                );
        if (order.isPresent()) {
            return readTopBannerPayload(order.get().getPayloadJson());
        }
        return resolveFallbackTopBannerItems(slotCode);
    }

    private List<OpsDiscoverConfigResponse.CommunityItemConfig> resolveCommunityItemsForSlot() {
        List<OpsDiscoverConfigResponse.CommunityItemConfig> experimentVariant = resolveExperimentCommunityVariant(SLOT_COMMUNITY_TODAY);
        if (!experimentVariant.isEmpty()) {
            return experimentVariant;
        }
        Optional<OpsDiscoverPublishOrder> order = publishOrderRepository
                .findTopByScopeCodeAndStatusInAndEffectiveAtLessThanEqualOrderByEffectiveAtDescCreatedAtDesc(
                        SCOPE_COMMUNITY,
                        List.of(ORDER_STATUS_PUBLISHED, ORDER_STATUS_SCHEDULED),
                        LocalDateTime.now()
                );
        if (order.isPresent()) {
            return readCommunityPayload(order.get().getPayloadJson());
        }
        return resolveFallbackCommunityItems();
    }

    private List<OpsDiscoverConfigResponse.TopBannerConfig> resolveExperimentTopBannerVariant(String slotCode) {
        OpsDiscoverExperiment experiment = pickExperiment(slotCode).orElse(null);
        if (experiment == null || !isInExperiment(experiment)) {
            return List.of();
        }
        return readTopBannerPayload(experiment.getVariantPayloadJson());
    }

    private List<OpsDiscoverConfigResponse.CommunityItemConfig> resolveExperimentCommunityVariant(String slotCode) {
        OpsDiscoverExperiment experiment = pickExperiment(slotCode).orElse(null);
        if (experiment == null || !isInExperiment(experiment)) {
            return List.of();
        }
        return readCommunityPayload(experiment.getVariantPayloadJson());
    }

    private Optional<OpsDiscoverExperiment> pickExperiment(String slotCode) {
        LocalDateTime now = LocalDateTime.now();
        return experimentRepository.findBySlotCodeOrderByUpdatedAtDesc(slotCode).stream()
                .filter(experiment -> EXPERIMENT_STATUS_ACTIVE.equalsIgnoreCase(experiment.getStatus()))
                .filter(experiment -> (experiment.getStartAt() == null || !experiment.getStartAt().isAfter(now))
                        && (experiment.getEndAt() == null || !experiment.getEndAt().isBefore(now)))
                .findFirst();
    }

    private boolean isInExperiment(OpsDiscoverExperiment experiment) {
        int bucket = ThreadLocalRandom.current().nextInt(100) + 1;
        return experiment.getTrafficPercent() != null && bucket <= experiment.getTrafficPercent();
    }

    private List<OpsDiscoverConfigResponse.TopBannerConfig> resolveFallbackTopBannerItems(String slotCode) {
        OpsContentSlot slot = opsContentSlotRepository.findBySlotCodeAndEnabledTrue(slotCode).orElse(null);
        if (slot == null) {
            return List.of();
        }
        return opsContentItemRepository.findActiveBySlotId(slot.getId(), LocalDateTime.now()).stream()
                .map(item -> new OpsDiscoverConfigResponse.TopBannerConfig(
                        item.getId(),
                        gameRepository.findById(item.getGameId()).map(Game::getAppId).orElse(""),
                        item.getTitle(),
                        item.getSubtitle(),
                        item.getBadgeText(),
                        item.getCoverUrl(),
                        item.getStatus(),
                        item.getStartAt(),
                        item.getEndAt(),
                        item.getSortOrder()
                ))
                .toList();
    }

    private List<OpsContentItem> resolveFallbackSlotItems(String slotCode) {
        OpsContentSlot slot = opsContentSlotRepository.findBySlotCodeAndEnabledTrue(slotCode).orElse(null);
        if (slot == null) {
            return List.of();
        }
        return opsContentItemRepository.findActiveBySlotId(slot.getId(), LocalDateTime.now());
    }

    private List<OpsDiscoverConfigResponse.CommunityItemConfig> resolveFallbackCommunityItems() {
        return resolveFallbackSlotItems(SLOT_COMMUNITY_TODAY).stream()
                .map(item -> new OpsDiscoverConfigResponse.CommunityItemConfig(
                        item.getId(),
                        gameRepository.findById(item.getGameId()).map(Game::getAppId).orElse(""),
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
                ))
                .toList();
    }

    private String slotToScope(String slotCode) {
        return switch (slotCode) {
            case SLOT_DISCOVER_HERO -> SCOPE_DISCOVER_BANNERS;
            case SLOT_LIBRARY_COLDSTART_HERO -> SCOPE_GAME_BANNERS;
            case SLOT_COMMUNITY_TODAY -> SCOPE_COMMUNITY;
            default -> null;
        };
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

    private String textOrFallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback == null ? "" : fallback;
        }
        return value;
    }

    private String resolveDiscoverCoverUrl(Game game) {
        GameOpsProfile profile = gameOpsProfileRepository.findByGameId(game.getId()).orElse(null);
        if (profile != null && profile.getDiscoverCardCoverUrl() != null && !profile.getDiscoverCardCoverUrl().isBlank()) {
            return profile.getDiscoverCardCoverUrl();
        }
        return game.getIconUrl();
    }

    private String resolveCommunityIconUrl(Game game) {
        if (game.getIconUrl() != null && !game.getIconUrl().isBlank()) {
            return game.getIconUrl();
        }
        GameOpsProfile profile = gameOpsProfileRepository.findByGameId(game.getId()).orElse(null);
        if (profile == null) {
            return "";
        }
        if (profile.getDiscoverCardLogoUrl() != null && !profile.getDiscoverCardLogoUrl().isBlank()) {
            return profile.getDiscoverCardLogoUrl();
        }
        if (profile.getRuntimeLogoUrl() != null && !profile.getRuntimeLogoUrl().isBlank()) {
            return profile.getRuntimeLogoUrl();
        }
        if (profile.getDiscoverCardCoverUrl() != null && !profile.getDiscoverCardCoverUrl().isBlank()) {
            return profile.getDiscoverCardCoverUrl();
        }
        return "";
    }

    private void normalizeClientGame(Game game) {
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
    }

    private String buildControlPlaneDownloadUrl(String appId) {
        return publicBaseUrl.replaceAll("/+$", "") + "/game/download/" + appId;
    }
}
