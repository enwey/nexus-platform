package com.nexus.platform.repository;

import com.nexus.platform.entity.GameOpsProfile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameOpsProfileRepository extends JpaRepository<GameOpsProfile, Long> {
    Optional<GameOpsProfile> findByGameId(Long gameId);
    List<GameOpsProfile> findByGameIdIn(Collection<Long> gameIds);
}
