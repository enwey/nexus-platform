package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsDiscoverExperiment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsDiscoverExperimentRepository extends JpaRepository<OpsDiscoverExperiment, Long> {
    List<OpsDiscoverExperiment> findTop20ByOrderByUpdatedAtDesc();

    List<OpsDiscoverExperiment> findByScopeCodeOrderByUpdatedAtDesc(String scopeCode);

    List<OpsDiscoverExperiment> findBySlotCodeOrderByUpdatedAtDesc(String slotCode);
}
