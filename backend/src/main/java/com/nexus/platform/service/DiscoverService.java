package com.nexus.platform.service;

import com.nexus.platform.dto.DiscoverFeedItem;
import com.nexus.platform.dto.DiscoverHeroCard;
import com.nexus.platform.dto.DiscoverHomeResponse;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.GameOpsProfile;
import com.nexus.platform.entity.OpsCollection;
import com.nexus.platform.entity.OpsCollectionGameRel;
import com.nexus.platform.entity.OpsContentItem;
import com.nexus.platform.entity.OpsContentSlot;
import com.nexus.platform.repository.GameOpsProfileRepository;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.OpsCollectionGameRelRepository;
import com.nexus.platform.repository.OpsCollectionRepository;
import com.nexus.platform.repository.OpsContentItemRepository;
import com.nexus.platform.repository.OpsContentSlotRepository;
import com.nexus.platform.repository.UserGameActionLogRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiscoverService {
    private static final String SLOT_DISCOVER_HERO = "DISCOVER_HERO";
    private static final String SLOT_DISCOVER_RANK = "DISCOVER_RANK";
    private static final String COLLECTION_NEWBIE_MUST_PLAY = "NEWBIE_MUST_PLAY";

    private final GameRepository gameRepository;
    private final GameOpsProfileRepository gameOpsProfileRepository;
    private final OpsContentSlotRepository opsContentSlotRepository;
    private final OpsContentItemRepository opsContentItemRepository;
    private final OpsCollectionRepository opsCollectionRepository;
    private final OpsCollectionGameRelRepository opsCollectionGameRelRepository;
    private final UserGameActionLogRepository actionLogRepository;

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
        DiscoverHeroCard hero = resolveHeroCard();

        List<DiscoverFeedItem> ranked = resolveSlotGames(SLOT_DISCOVER_RANK, normalizedLimit).stream()
                .map(this::toDiscoverFeedItemWithOpsProfile)
                .toList();
        if (ranked.isEmpty()) {
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

        if (hero == null && !ranked.isEmpty()) {
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
        return Result.success(new DiscoverHomeResponse(hero, ranked, newbie, everyone));
    }

    private DiscoverFeedItem toDiscoverFeedItemWithOpsProfile(Game game) {
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
            return game.getCategory().toLowerCase(Locale.ROOT);
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

    private DiscoverHeroCard resolveHeroCard() {
        OpsContentSlot heroSlot = opsContentSlotRepository.findBySlotCodeAndEnabledTrue(SLOT_DISCOVER_HERO).orElse(null);
        if (heroSlot == null) {
            return null;
        }
        List<OpsContentItem> items = opsContentItemRepository.findActiveBySlotId(heroSlot.getId(), LocalDateTime.now());
        if (items.isEmpty()) {
            return null;
        }
        OpsContentItem first = items.get(0);
        Game game = gameRepository.findById(first.getGameId()).orElse(null);
        if (game == null || game.getStatus() != Game.GameStatus.APPROVED) {
            return null;
        }
        String title = textOrFallback(first.getTitle(), game.getName());
        String subtitle = textOrFallback(first.getSubtitle(), game.getDescription());
        String coverUrl = textOrFallback(first.getCoverUrl(), resolveDiscoverCoverUrl(game));
        String badge = textOrFallback(first.getBadgeText(), "Recommended");
        return new DiscoverHeroCard(game.getAppId(), title, subtitle, coverUrl, badge, "OPEN_GAME");
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
}
