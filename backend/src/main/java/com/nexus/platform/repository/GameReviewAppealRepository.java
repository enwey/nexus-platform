package com.nexus.platform.repository;

import com.nexus.platform.entity.GameReviewAppeal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameReviewAppealRepository extends JpaRepository<GameReviewAppeal, Long> {
    List<GameReviewAppeal> findByDeveloperIdOrderByCreatedAtDesc(Long developerId);

    List<GameReviewAppeal> findByGameIdOrderByCreatedAtDesc(Long gameId);

    boolean existsByVersionIdAndAppealStatus(Long versionId, String appealStatus);

    List<GameReviewAppeal> findAllByOrderByUpdatedAtDesc();
}
