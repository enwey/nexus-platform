package com.nexus.platform.service;

import com.nexus.platform.dto.OpsDiscoverCategoryRequest;
import com.nexus.platform.dto.OpsDiscoverCategoryResponse;
import com.nexus.platform.dto.OpsDiscoverConfigResponse;
import com.nexus.platform.dto.OpsDiscoverConfigUpdateRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.OpsCollection;
import com.nexus.platform.entity.OpsCollectionGameRel;
import com.nexus.platform.entity.OpsContentItem;
import com.nexus.platform.entity.OpsContentSlot;
import com.nexus.platform.entity.OpsDiscoverCategory;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.OpsCollectionGameRelRepository;
import com.nexus.platform.repository.OpsCollectionRepository;
import com.nexus.platform.repository.OpsContentItemRepository;
import com.nexus.platform.repository.OpsContentSlotRepository;
import com.nexus.platform.repository.OpsDiscoverCategoryRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final GameRepository gameRepository;
    private final OpsContentSlotRepository slotRepository;
    private final OpsContentItemRepository itemRepository;
    private final OpsCollectionRepository collectionRepository;
    private final OpsCollectionGameRelRepository collectionGameRelRepository;
    private final OpsDiscoverCategoryRepository categoryRepository;

    @Transactional
    public Result<OpsDiscoverConfigResponse> getConfig() {
        List<Game> games = gameRepository.findByStatusOrderByCreatedAtDesc(Game.GameStatus.APPROVED);
        Map<Long, Game> gameById = games.stream().collect(Collectors.toMap(Game::getId, Function.identity()));
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
                            game == null ? "" : game.getAppId(),
                            item.getTitle(),
                            item.getSubtitle(),
                            item.getBadgeText(),
                            item.getCoverUrl()
                    );
                })
                .toList();
        List<OpsDiscoverConfigResponse.TopBannerConfig> gameTopBanners = gameTopRows.stream()
                .map(item -> {
                    Game game = gameById.get(item.getGameId());
                    return new OpsDiscoverConfigResponse.TopBannerConfig(
                            game == null ? "" : game.getAppId(),
                            item.getTitle(),
                            item.getSubtitle(),
                            item.getBadgeText(),
                            item.getCoverUrl()
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
            seedDefaultCommunityItems(communitySlot.getId(), games);
            communityRows = itemRepository.findBySlotIdOrderBySortOrderAscUpdatedAtDesc(communitySlot.getId());
        }

        List<OpsDiscoverConfigResponse.CommunityItemConfig> communityItems =
                communityRows.stream()
                        .map(item -> {
                            Game game = gameById.get(item.getGameId());
                            return new OpsDiscoverConfigResponse.CommunityItemConfig(
                                    game == null ? "" : game.getAppId(),
                                    item.getBadgeText(),
                                    item.getTitle(),
                                    item.getCoverUrl(),
                                    item.getArticleTag(),
                                    item.getArticleTitle(),
                                    item.getArticleBody(),
                                    item.getActionText()
                            );
                        })
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

        List<OpsDiscoverConfigResponse.CategoryOption> categoryOptions = buildCategoryOptions();
        return Result.success(new OpsDiscoverConfigResponse(
                hero,
                gameTopBanners,
                discoverTopBanners,
                ranked,
                newbie,
                everyone,
                communityItems,
                availableGames,
                categoryOptions
        ));
    }

    @Transactional
    public Result<OpsDiscoverConfigResponse> updateConfig(OpsDiscoverConfigUpdateRequest request, User currentUser) {
        if (request == null) {
            return Result.error("Request body is required");
        }
        ensureDefaultCategorySeed();

        List<Game> games = gameRepository.findByStatusOrderByCreatedAtDesc(Game.GameStatus.APPROVED);
        Map<String, Game> gameByAppId = games.stream().collect(Collectors.toMap(Game::getAppId, Function.identity()));

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
                    request.hero().coverUrl()
            ));
        }
        String saveDiscoverTopBannerError = saveTopBannerItems(
                heroSlot.getId(),
                discoverTopBanners,
                gameByAppId,
                validCategoryNames
        );
        if (saveDiscoverTopBannerError != null) {
            return Result.error(saveDiscoverTopBannerError);
        }

        itemRepository.deleteBySlotId(rankSlot.getId());
        saveSlotItems(rankSlot.getId(), request.rankedAppIds(), gameByAppId);
        itemRepository.deleteBySlotId(communitySlot.getId());
        String saveCommunityError =
                saveCommunityItems(communitySlot.getId(), request.communityItems(), gameByAppId, validCategoryNames);
        if (saveCommunityError != null) {
            return Result.error(saveCommunityError);
        }

        replaceCollectionRelations(newbieCollection, request.newbieAppIds(), gameByAppId, currentUser);
        replaceCollectionRelations(everyoneCollection, request.everyoneAppIds(), gameByAppId, currentUser);

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
            OpsContentSlot communitySlot = ensureSlot(SLOT_COMMUNITY_TODAY, "Community Today", "COMMUNITY", "TODAY");
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
        long usedCount = itemRepository.countBySlotIdAndBadgeText(communitySlot.getId(), row.getName());
        if (usedCount > 0) {
            return Result.error("Category is used by recommendations and cannot be deleted");
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
        int sort = 0;
        for (OpsDiscoverConfigUpdateRequest.CommunityItemConfig cfg : items) {
            if (cfg == null || cfg.appId() == null || cfg.appId().isBlank()) {
                continue;
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
            item.setStatus("PUBLISHED");
            item.setSortOrder(sort++);
            itemRepository.save(item);
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
        int sort = 0;
        for (OpsDiscoverConfigUpdateRequest.TopBannerConfig cfg : items) {
            if (cfg == null || cfg.appId() == null || cfg.appId().isBlank()) {
                continue;
            }
            Game game = gameByAppId.get(cfg.appId());
            if (game == null) {
                return "Top banner game appId is invalid: " + cfg.appId();
            }
            String badgeText = normalizeCategoryName(cfg.badgeText());
            if (badgeText != null && !validCategoryNames.contains(badgeText)) {
                return "Top banner category does not exist: " + badgeText;
            }
            OpsContentItem item = new OpsContentItem();
            item.setSlotId(slotId);
            item.setGameId(game.getId());
            item.setTitle(cfg.title());
            item.setSubtitle(cfg.subtitle());
            item.setBadgeText(badgeText);
            item.setCoverUrl(cfg.coverUrl());
            item.setStatus("PUBLISHED");
            item.setSortOrder(sort++);
            itemRepository.save(item);
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

    private OpsDiscoverCategoryResponse toCategoryResponse(OpsDiscoverCategory row) {
        return new OpsDiscoverCategoryResponse(row.getId(), row.getName(), row.getSortOrder());
    }

    private Integer categorySortOrder(Integer inputSortOrder) {
        return inputSortOrder == null ? 0 : Math.max(0, inputSortOrder);
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
}
