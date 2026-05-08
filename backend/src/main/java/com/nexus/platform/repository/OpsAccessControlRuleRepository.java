package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsAccessControlRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsAccessControlRuleRepository extends JpaRepository<OpsAccessControlRule, Long> {
    List<OpsAccessControlRule> findAllByOrderByUpdatedAtDesc();

    Optional<OpsAccessControlRule> findByRuleTypeAndTargetValue(String ruleType, String targetValue);
}
