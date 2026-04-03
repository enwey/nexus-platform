package com.nexus.platform.repository;

import com.nexus.platform.entity.Game;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByDeveloperId(Long developerId);

    List<Game> findByStatus(Game.GameStatus status);
    Page<Game> findByStatus(Game.GameStatus status, Pageable pageable);
    List<Game> findByStatusOrderByCreatedAtDesc(Game.GameStatus status);

    List<Game> findAllByOrderByCreatedAtDesc();
    Page<Game> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Game> findByDeveloperIdOrderByCreatedAtDesc(Long developerId);
    Page<Game> findByDeveloperIdOrderByCreatedAtDesc(Long developerId, Pageable pageable);

    Game findByAppId(String appId);
}
