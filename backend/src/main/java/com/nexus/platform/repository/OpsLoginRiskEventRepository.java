package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsLoginRiskEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsLoginRiskEventRepository extends JpaRepository<OpsLoginRiskEvent, Long> {
    List<OpsLoginRiskEvent> findTop200ByOrderByCreatedAtDesc();

    long countByClientIpAndCreatedAtAfter(String clientIp, LocalDateTime createdAt);
}
