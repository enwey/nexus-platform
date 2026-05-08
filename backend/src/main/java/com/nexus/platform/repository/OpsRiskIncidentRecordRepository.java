package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsRiskIncidentRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsRiskIncidentRecordRepository extends JpaRepository<OpsRiskIncidentRecord, Long> {
    List<OpsRiskIncidentRecord> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
}
