package com.nexus.platform.repository;

import com.nexus.platform.entity.AndroidFeatureToggle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AndroidFeatureToggleRepository extends JpaRepository<AndroidFeatureToggle, Long> {
    List<AndroidFeatureToggle> findAllByOrderByUpdatedAtDesc();
    Optional<AndroidFeatureToggle> findByFeatureKey(String featureKey);
}
