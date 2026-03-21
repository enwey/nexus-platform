package com.nexus.platform.repository;

import com.nexus.platform.entity.Game;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByDeveloperId(Long developerId);

    List<Game> findByStatus(Game.GameStatus status);

    List<Game> findAllByOrderByCreatedAtDesc();

    List<Game> findByDeveloperIdOrderByCreatedAtDesc(Long developerId);

    Game findByAppId(String appId);
}
