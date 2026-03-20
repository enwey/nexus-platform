package com.nexus.platform.repository;

import com.nexus.platform.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByDeveloperId(Long developerId);
    List<Game> findByStatus(Game.GameStatus status);
    Game findByAppId(String appId);
}
