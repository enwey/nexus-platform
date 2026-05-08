package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsRiskRuleConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsRiskRuleConfigRepository extends JpaRepository<OpsRiskRuleConfig, Long> {
    List<OpsRiskRuleConfig> findAllByOrderByUpdatedAtDesc();

    Optional<OpsRiskRuleConfig> findByRuleCode(String ruleCode);
}
