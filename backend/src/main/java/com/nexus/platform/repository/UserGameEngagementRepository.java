package com.nexus.platform.repository;

import com.nexus.platform.entity.UserGameEngagement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserGameEngagementRepository extends JpaRepository<UserGameEngagement, Long> {
    List<UserGameEngagement> findByUserId(Long userId);

    Optional<UserGameEngagement> findByUserIdAndAppId(Long userId, String appId);

    List<UserGameEngagement> findByUserIdAndLastPlayedAtIsNotNullOrderByLastPlayedAtDesc(Long userId);

    List<UserGameEngagement> findByUserIdAndFavoriteTrueOrderByLastPlayedAtDescFavoriteAtDesc(Long userId);

    @Query("""
            SELECT e.appId AS appId, COUNT(e.id) AS playerCount
            FROM UserGameEngagement e
            WHERE e.appId IN :appIds
              AND COALESCE(e.playCount, 0) > 0
            GROUP BY e.appId
            """)
    List<GamePlayerCountAggregate> aggregatePlayerCountByAppIds(@Param("appIds") List<String> appIds);

    interface GamePlayerCountAggregate {
        String getAppId();
        Long getPlayerCount();
    }
}
