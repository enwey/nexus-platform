package com.nexus.platform.repository;

import com.nexus.platform.entity.UserGameEngagement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserGameEngagementRepository extends JpaRepository<UserGameEngagement, Long> {
    List<UserGameEngagement> findByUserId(Long userId);

    Optional<UserGameEngagement> findByUserIdAndAppId(Long userId, String appId);

    List<UserGameEngagement> findByUserIdAndLastPlayedAtIsNotNullOrderByLastPlayedAtDesc(Long userId);

    List<UserGameEngagement> findByUserIdAndFavoriteTrueOrderByLastPlayedAtDescFavoriteAtDesc(Long userId);
}
