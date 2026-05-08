package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsRiskIncident;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OpsRiskIncidentRepository extends JpaRepository<OpsRiskIncident, Long> {
    List<OpsRiskIncident> findByStatusInOrderByUpdatedAtDesc(List<String> statuses);
}
