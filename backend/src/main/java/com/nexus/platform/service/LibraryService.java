package com.nexus.platform.service;

import com.nexus.platform.dto.LibraryHomeResponse;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.OpsCollection;
import com.nexus.platform.entity.OpsCollectionGameRel;
import com.nexus.platform.entity.User;
import com.nexus.platform.entity.UserGameActionLog;
import com.nexus.platform.entity.UserGameEngagement;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.OpsCollectionGameRelRepository;
import com.nexus.platform.repository.OpsCollectionRepository;
import com.nexus.platform.repository.UserGameActionLogRepository;
import com.nexus.platform.repository.UserGameEngagementRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryService {
    private static final String COLLECTION_NEWBIE_MUST_PLAY = "NEWBIE_MUST_PLAY";
    private static final String COLLECTION_EVERYONE_PLAYING = "EVERYONE_PLAYING";

    private final GameRepository gameRepository;
    private final UserGameEngagementRepository engagementRepository;
    private final OpsCollectionRepository opsCollectionRepository;
    private final OpsCollectionGameRelRepository opsCollectionGameRelRepository;
    private final UserGameActionLogRepository actionLogRepository;

    public Result<LibraryHomeResponse> getHome(User currentUser) {
        Long userId = currentUser.getId();
        List<Game> recentGames = resolveRecentGames(userId);
        List<Game> myGames = resolveFavoriteGames(userId);
        Game current = recentGames.isEmpty() ? null : recentGames.get(0);
        List<Game> newbieMustPlay = resolveCollectionGames(COLLECTION_NEWBIE_MUST_PLAY, 12);
        if (newbieMustPlay.isEmpty()) {
            newbieMustPlay = getDefaultApprovedGames(12);
        }
        List<Game> everyonePlaying = resolveEveryonePlayingGames(12);
        long favoriteCount = myGames.size();
        long shareCount = engagementRepository.findByUserId(userId).stream()
                .mapToLong(row -> Optional.ofNullable(row.getShareCount()).orElse(0L))
                .sum();
        return Result.success(new LibraryHomeResponse(
                current,
                recentGames,
                myGames,
                newbieMustPlay,
                everyonePlaying,
                favoriteCount,
                shareCount
        ));
    }

    @Transactional
    public Result<Void> markPlayed(User currentUser, String appId) {
        Game game = findApprovedGame(appId);
        if (game == null) {
            return Result.error("Game not found");
        }
        UserGameEngagement engagement = engagementRepository
                .findByUserIdAndAppId(currentUser.getId(), appId)
                .orElseGet(() -> createEngagement(currentUser.getId(), appId));
        long nextPlayCount = Optional.ofNullable(engagement.getPlayCount()).orElse(0L) + 1L;
        engagement.setPlayCount(nextPlayCount);
        engagement.setLastPlayedAt(LocalDateTime.now());
        engagementRepository.save(engagement);
        saveActionLog(currentUser.getId(), game, "PLAY", "LIBRARY_HOME");
        return Result.success();
    }

    @Transactional
    public Result<Void> setFavorite(User currentUser, String appId, boolean favorite) {
        Game game = findApprovedGame(appId);
        if (game == null) {
            return Result.error("Game not found");
        }
        UserGameEngagement engagement = engagementRepository
                .findByUserIdAndAppId(currentUser.getId(), appId)
                .orElseGet(() -> createEngagement(currentUser.getId(), appId));
        engagement.setFavorite(favorite);
        engagement.setFavoriteAt(favorite ? LocalDateTime.now() : null);
        engagementRepository.save(engagement);
        saveActionLog(currentUser.getId(), game, favorite ? "FAVORITE" : "UNFAVORITE", "LIBRARY_HOME");
        return Result.success();
    }

    @Transactional
    public Result<Void> markShared(User currentUser, String appId) {
        Game game = findApprovedGame(appId);
        if (game == null) {
            return Result.error("Game not found");
        }
        UserGameEngagement engagement = engagementRepository
                .findByUserIdAndAppId(currentUser.getId(), appId)
                .orElseGet(() -> createEngagement(currentUser.getId(), appId));
        long nextShareCount = Optional.ofNullable(engagement.getShareCount()).orElse(0L) + 1L;
        engagement.setShareCount(nextShareCount);
        engagement.setLastSharedAt(LocalDateTime.now());
        engagementRepository.save(engagement);
        saveActionLog(currentUser.getId(), game, "SHARE", "GAME_RUNTIME");
        return Result.success();
    }

    private UserGameEngagement createEngagement(Long userId, String appId) {
        UserGameEngagement engagement = new UserGameEngagement();
        engagement.setUserId(userId);
        engagement.setAppId(appId);
        engagement.setPlayCount(0L);
        engagement.setFavorite(false);
        engagement.setShareCount(0L);
        return engagement;
    }

    private List<Game> resolveRecentGames(Long userId) {
        List<UserGameEngagement> rows = engagementRepository
                .findByUserIdAndLastPlayedAtIsNotNullOrderByLastPlayedAtDesc(userId);
        return mapToApprovedGamesInOrder(rows);
    }

    private List<Game> resolveFavoriteGames(Long userId) {
        List<UserGameEngagement> rows = engagementRepository
                .findByUserIdAndFavoriteTrueOrderByLastPlayedAtDescFavoriteAtDesc(userId);
        return mapToApprovedGamesInOrder(rows);
    }

    private List<Game> mapToApprovedGamesInOrder(List<UserGameEngagement> rows) {
        LinkedHashSet<String> appIds = new LinkedHashSet<>();
        for (UserGameEngagement row : rows) {
            if (row.getAppId() != null && !row.getAppId().isBlank()) {
                appIds.add(row.getAppId());
            }
        }
        List<Game> games = new ArrayList<>();
        for (String appId : appIds) {
            Game game = gameRepository.findByAppId(appId);
            if (game != null && game.getStatus() == Game.GameStatus.APPROVED) {
                games.add(game);
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
        List<Game> curated = resolveCollectionGames(COLLECTION_EVERYONE_PLAYING, limit);
        if (!curated.isEmpty()) {
            return curated;
        }

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

    private Game findApprovedGame(String appId) {
        Game game = gameRepository.findByAppId(appId);
        if (game == null || game.getStatus() != Game.GameStatus.APPROVED) {
            return null;
        }
        return game;
    }

    private void saveActionLog(Long userId, Game game, String actionType, String scene) {
        UserGameActionLog log = new UserGameActionLog();
        log.setUserId(userId);
        log.setGameId(game.getId());
        log.setAppId(game.getAppId());
        log.setActionType(actionType);
        log.setScene(scene);
        actionLogRepository.save(log);
    }
}
