package com.nexus.platform.repository;

import com.nexus.platform.entity.DeveloperCertificationReviewRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeveloperCertificationReviewRecordRepository extends JpaRepository<DeveloperCertificationReviewRecord, Long> {
    List<DeveloperCertificationReviewRecord> findByDeveloperIdOrderByCreatedAtDesc(Long developerId);
}
