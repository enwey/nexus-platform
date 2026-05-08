package com.nexus.platform.repository;

import com.nexus.platform.entity.AndroidCompatibilityBlacklist;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AndroidCompatibilityBlacklistRepository extends JpaRepository<AndroidCompatibilityBlacklist, Long> {
    Optional<AndroidCompatibilityBlacklist> findByRuleCode(String ruleCode);

    List<AndroidCompatibilityBlacklist> findAllByOrderByUpdatedAtDesc();
}
