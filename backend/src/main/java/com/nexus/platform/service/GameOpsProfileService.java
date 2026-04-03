package com.nexus.platform.service;

import com.nexus.platform.dto.GameOpsDtos.GameOpsProfileResponse;
import com.nexus.platform.dto.GameOpsDtos.GameOpsProfileUpdateRequest;
import com.nexus.platform.dto.GameOpsDtos.RuntimeProfileResponse;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.GameOpsProfile;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.GameOpsProfileRepository;
import com.nexus.platform.repository.GameRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameOpsProfileService {
    private final GameRepository gameRepository;
    private final GameOpsProfileRepository gameOpsProfileRepository;

    public Result<GameOpsProfileResponse> getProfile(Long gameId) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            return Result.error("Game not found");
        }
        return Result.success(toResponse(gameId, gameOpsProfileRepository.findByGameId(gameId).orElse(null)));
    }

    public Result<GameOpsProfileResponse> updateProfile(Long gameId, GameOpsProfileUpdateRequest request, User currentUser) {
        if (request == null) {
            return Result.error("Request body is required");
        }
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            return Result.error("Game not found");
        }

        GameOpsProfile profile = gameOpsProfileRepository.findByGameId(gameId).orElseGet(() -> {
            GameOpsProfile created = new GameOpsProfile();
            created.setGameId(gameId);
            return created;
        });

        profile.setStudioName(trimToNull(request.studioName()));
        profile.setPlayerCountText(trimToNull(request.playerCountText()));
        profile.setRuntimeBannerUrl(trimToNull(request.runtimeBannerUrl()));
        profile.setRuntimeLogoUrl(trimToNull(request.runtimeLogoUrl()));
        profile.setShareTitle(trimToNull(request.shareTitle()));
        profile.setShareSubtitle(trimToNull(request.shareSubtitle()));
        profile.setShareImageUrl(trimToNull(request.shareImageUrl()));
        profile.setDiscoverCardCoverUrl(trimToNull(request.discoverCardCoverUrl()));
        profile.setDiscoverCardLogoUrl(trimToNull(request.discoverCardLogoUrl()));
        profile.setUpdatedBy(currentUser == null ? null : currentUser.getId());

        GameOpsProfile saved = gameOpsProfileRepository.save(profile);
        return Result.success(toResponse(gameId, saved));
    }

    public Result<RuntimeProfileResponse> getRuntimeProfile(String appId) {
        Game game = gameRepository.findByAppId(appId);
        if (game == null || game.getStatus() != Game.GameStatus.APPROVED) {
            return Result.error("Game not found");
        }

        Optional<GameOpsProfile> profileOpt = gameOpsProfileRepository.findByGameId(game.getId());
        GameOpsProfile profile = profileOpt.orElse(null);
        String studio = profile == null || profile.getStudioName() == null || profile.getStudioName().isBlank()
                ? "Nexus Studio"
                : profile.getStudioName();
        String players = profile == null || profile.getPlayerCountText() == null || profile.getPlayerCountText().isBlank()
                ? "2.4M+"
                : profile.getPlayerCountText();
        String shareTitle = profile == null || profile.getShareTitle() == null || profile.getShareTitle().isBlank()
                ? game.getName()
                : profile.getShareTitle();
        String shareSubtitle = profile == null || profile.getShareSubtitle() == null || profile.getShareSubtitle().isBlank()
                ? game.getDescription()
                : profile.getShareSubtitle();

        RuntimeProfileResponse response = new RuntimeProfileResponse(
                game.getAppId(),
                game.getName(),
                studio,
                players,
                profile == null ? null : profile.getRuntimeBannerUrl(),
                profile == null ? null : profile.getRuntimeLogoUrl(),
                shareTitle,
                shareSubtitle,
                profile == null ? null : profile.getShareImageUrl()
        );
        return Result.success(response);
    }

    private GameOpsProfileResponse toResponse(Long gameId, GameOpsProfile profile) {
        if (profile == null) {
            return new GameOpsProfileResponse(gameId, null, null, null, null, null, null, null, null, null);
        }
        return new GameOpsProfileResponse(
                gameId,
                profile.getStudioName(),
                profile.getPlayerCountText(),
                profile.getRuntimeBannerUrl(),
                profile.getRuntimeLogoUrl(),
                profile.getShareTitle(),
                profile.getShareSubtitle(),
                profile.getShareImageUrl(),
                profile.getDiscoverCardCoverUrl(),
                profile.getDiscoverCardLogoUrl()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
