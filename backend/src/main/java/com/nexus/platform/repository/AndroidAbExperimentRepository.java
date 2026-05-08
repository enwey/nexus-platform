package com.nexus.platform.repository;

import com.nexus.platform.entity.AndroidAbExperiment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AndroidAbExperimentRepository extends JpaRepository<AndroidAbExperiment, Long> {
    List<AndroidAbExperiment> findAllByOrderByUpdatedAtDesc();
    Optional<AndroidAbExperiment> findByExperimentKey(String experimentKey);
}
