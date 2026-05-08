package com.nexus.platform.repository;

import com.nexus.platform.entity.DeveloperGameMetricDaily;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeveloperGameMetricDailyRepository extends JpaRepository<DeveloperGameMetricDaily, Long> {
    List<DeveloperGameMetricDaily> findByDeveloperIdAndMetricDateGreaterThanEqualOrderByMetricDateAsc(Long developerId, LocalDate metricDate);
}
