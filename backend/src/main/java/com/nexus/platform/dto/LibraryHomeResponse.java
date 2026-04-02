package com.nexus.platform.dto;

import com.nexus.platform.entity.Game;
import java.util.List;

public record LibraryHomeResponse(
        Game currentPlayingGame,
        List<Game> recentGames,
        List<Game> myGames,
        List<Game> newbieMustPlay,
        List<Game> everyonePlaying,
        long favoriteCount,
        long shareCount
) {
}
