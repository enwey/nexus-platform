package com.nexus.platform.repository;

import com.nexus.platform.entity.DeveloperGovernanceRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeveloperGovernanceRecordRepository extends JpaRepository<DeveloperGovernanceRecord, Long> {
    List<DeveloperGovernanceRecord> findByDeveloperIdOrderByCreatedAtDesc(Long developerId);
}
