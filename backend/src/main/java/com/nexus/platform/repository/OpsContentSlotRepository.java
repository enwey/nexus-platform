package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsContentSlot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OpsContentSlotRepository extends JpaRepository<OpsContentSlot, Long> {
    Optional<OpsContentSlot> findBySlotCode(String slotCode);
    Optional<OpsContentSlot> findBySlotCodeAndEnabledTrue(String slotCode);
}
