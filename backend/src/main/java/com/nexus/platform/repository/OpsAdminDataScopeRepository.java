package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsAdminDataScope;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsAdminDataScopeRepository extends JpaRepository<OpsAdminDataScope, Long> {
    Optional<OpsAdminDataScope> findByAdminUserId(Long adminUserId);
}
