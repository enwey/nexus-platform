package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsRolePermissionOverride;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsRolePermissionOverrideRepository extends JpaRepository<OpsRolePermissionOverride, Long> {
    List<OpsRolePermissionOverride> findAllByOrderByRoleCodeAscPermissionCodeAsc();
    void deleteByRoleCode(String roleCode);
}
