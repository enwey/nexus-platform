package com.nexus.platform.service;

import com.nexus.platform.dto.OpsDiscoverConfigResponse;
import com.nexus.platform.dto.OpsDiscoverConfigUpdateRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.OpsCollection;
import com.nexus.platform.entity.OpsCollectionGameRel;
import com.nexus.platform.entity.OpsContentItem;
import com.nexus.platform.entity.OpsContentSlot;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.OpsCollectionGameRelRepository;
import com.nexus.platform.repository.OpsCollectionRepository;
import com.nexus.platform.repository.OpsContentItemRepository;
import com.nexus.platform.repository.OpsContentSlotRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpsDiscoverAdminService {
    private static final String SLOT_DISCOVER_HERO = "DISCOVER_HERO";
    private static final String SLOT_DISCOVER_RANK = "DISCOVER_RANK";
    private static final String COLLECTION_NEWBIE_MUST_PLAY = "NEWBIE_MUST_PLAY";
    private static final String COLLECTION_EVERYONE_PLAYING = "EVERYONE_PLAYING";

    private final GameRepository gameRepository;
    private final OpsContentSlotRepository slotRepository;
    private final OpsContentItemRepository itemRepository;
    private final OpsCollectionRepository collectionRepository;
    private final OpsCollectionGameRelRepository collectionGameRelRepository;

    public Result<OpsDiscoverConfigResponse> getConfig() {
        List<Game> games = gameRepository.findByStatusOrderByCreatedAtDesc(Game.GameStatus.APPROVED);
        Map<Long, Game> gameById = games.stream().collect(Collectors.toMap(Game::getId, Function.identity()));

        OpsContentSlot heroSlot = ensureSlot(SLOT_DISCOVER_HERO, "Discover Hero", "DISCOVER", "HERO");
        OpsContentSlot rankSlot = ensureSlot(SLOT_DISCOVER_RANK, "Discover Rank", "DISCOVER", "RANK");
        OpsCollection newbieCollection = ensureCollection(COLLECTION_NEWBIE_MUST_PLAY, "Newbie Must Play");
        OpsCollection everyoneCollection = ensureCollection(COLLECTION_EVERYONE_PLAYING, "Everyone Playing");

        OpsContentItem heroItem = itemRepository.findBySlotIdOrderBySortOrderAscUpdatedAtDesc(heroSlot.getId()).stream()
                .findFirst().orElse(null);
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

        List<OpsDiscoverConfigResponse.SimpleGameItem> availableGames = games.stream()
                .map(game -> new OpsDiscoverConfigResponse.SimpleGameItem(
                        game.getId(),
                        game.getAppId(),
                        game.getName(),
                        game.getCategory(),
                        game.getStatus() == null ? "" : game.getStatus().name()
                ))
                .toList();

        return Result.success(new OpsDiscoverConfigResponse(hero, ranked, newbie, everyone, availableGames));
    }

    @Transactional
    public Result<OpsDiscoverConfigResponse> updateConfig(OpsDiscoverConfigUpdateRequest request, User currentUser) {
        if (request == null) {
            return Result.error("Request body is required");
        }

        List<Game> games = gameRepository.findByStatusOrderByCreatedAtDesc(Game.GameStatus.APPROVED);
        Map<String, Game> gameByAppId = games.stream().collect(Collectors.toMap(Game::getAppId, Function.identity()));

        OpsContentSlot heroSlot = ensureSlot(SLOT_DISCOVER_HERO, "Discover Hero", "DISCOVER", "HERO");
        OpsContentSlot rankSlot = ensureSlot(SLOT_DISCOVER_RANK, "Discover Rank", "DISCOVER", "RANK");
        OpsCollection newbieCollection = ensureCollection(COLLECTION_NEWBIE_MUST_PLAY, "Newbie Must Play");
        OpsCollection everyoneCollection = ensureCollection(COLLECTION_EVERYONE_PLAYING, "Everyone Playing");

        itemRepository.deleteBySlotId(heroSlot.getId());
        if (request.hero() != null && request.hero().appId() != null && !request.hero().appId().isBlank()) {
            Game heroGame = gameByAppId.get(request.hero().appId());
            if (heroGame == null) {
                return Result.error("Hero game appId is invalid");
            }
            OpsContentItem heroItem = new OpsContentItem();
            heroItem.setSlotId(heroSlot.getId());
            heroItem.setGameId(heroGame.getId());
            heroItem.setTitle(request.hero().title());
            heroItem.setSubtitle(request.hero().subtitle());
            heroItem.setBadgeText(request.hero().badgeText());
            heroItem.setCoverUrl(request.hero().coverUrl());
            heroItem.setStatus("PUBLISHED");
            heroItem.setSortOrder(0);
            itemRepository.save(heroItem);
        }

        itemRepository.deleteBySlotId(rankSlot.getId());
        saveSlotItems(rankSlot.getId(), request.rankedAppIds(), gameByAppId);

        replaceCollectionRelations(newbieCollection, request.newbieAppIds(), gameByAppId, currentUser);
        replaceCollectionRelations(everyoneCollection, request.everyoneAppIds(), gameByAppId, currentUser);

        return getConfig();
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
}
