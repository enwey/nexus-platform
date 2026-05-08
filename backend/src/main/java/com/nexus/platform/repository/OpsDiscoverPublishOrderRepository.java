package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsDiscoverPublishOrder;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsDiscoverPublishOrderRepository extends JpaRepository<OpsDiscoverPublishOrder, Long> {
    List<OpsDiscoverPublishOrder> findTop20ByOrderByCreatedAtDesc();

    List<OpsDiscoverPublishOrder> findByScopeCodeOrderByCreatedAtDesc(String scopeCode);

    Optional<OpsDiscoverPublishOrder> findTopByScopeCodeAndStatusInAndEffectiveAtLessThanEqualOrderByEffectiveAtDescCreatedAtDesc(
            String scopeCode,
            Collection<String> statuses,
            LocalDateTime effectiveAt
    );

    Optional<OpsDiscoverPublishOrder> findTopByScopeCodeAndStatusAndEffectiveAtAfterOrderByEffectiveAtAscCreatedAtAsc(
            String scopeCode,
            String status,
            LocalDateTime effectiveAt
    );
}
