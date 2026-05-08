package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DeveloperReleaseHealthDto(
        Overview overview,
        List<GameReleaseItem> games
) {
    public record Overview(
            int totalGames,
            int liveGames,
            int processingGames,
            int pendingReviewGames,
            int blockedGames,
            int releasableGames
    ) {
    }

    public record GameReleaseItem(
            Long gameId,
            String appId,
            String gameName,
            String gameStatus,
            String visibilityStatus,
            String frontendState,
            String latestVersion,
            String latestVersionStatus,
            Boolean manifestValid,
            Boolean canSubmit,
            Boolean canRollback,
            String blockingReason,
            LocalDateTime lastUpdated
    ) {
    }
}
