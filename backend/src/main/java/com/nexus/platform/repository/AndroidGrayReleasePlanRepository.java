package com.nexus.platform.repository;

import com.nexus.platform.entity.AndroidGrayReleasePlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AndroidGrayReleasePlanRepository extends JpaRepository<AndroidGrayReleasePlan, Long> {
    List<AndroidGrayReleasePlan> findAllByOrderByUpdatedAtDesc();
    Optional<AndroidGrayReleasePlan> findByPlanCode(String planCode);
}
