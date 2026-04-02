package com.nexus.platform.repository;

import com.nexus.platform.entity.UserGameActionLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserGameActionLogRepository extends JpaRepository<UserGameActionLog, Long> {
    @Query("""
            SELECT l.gameId AS gameId, COUNT(l.id) AS actionCount
            FROM UserGameActionLog l
            WHERE l.gameId IS NOT NULL
              AND l.actionType = :actionType
              AND l.createdAt >= :since
            GROUP BY l.gameId
            ORDER BY COUNT(l.id) DESC
            """)
    List<GameActionAggregate> aggregateTopGamesByActionSince(
            @Param("actionType") String actionType,
            @Param("since") LocalDateTime since
    );

    interface GameActionAggregate {
        Long getGameId();
        Long getActionCount();
    }
}
