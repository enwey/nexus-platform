package com.nexus.platform.repository;

import com.nexus.platform.entity.GameVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameVersionRepository extends JpaRepository<GameVersion, Long> {
    long countByGameId(Long gameId);

    Optional<GameVersion> findTopByGameIdOrderByCreatedAtDesc(Long gameId);

    Optional<GameVersion> findTopByGameIdAndStatusOrderByCreatedAtDesc(
            Long gameId,
            GameVersion.VersionStatus status
    );

    Optional<GameVersion> findByIdAndGameId(Long id, Long gameId);

    List<GameVersion> findByGameIdOrderByCreatedAtDesc(Long gameId);
}
