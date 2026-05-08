package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsMenuPermissionProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsMenuPermissionProfileRepository extends JpaRepository<OpsMenuPermissionProfile, Long> {
    List<OpsMenuPermissionProfile> findAllByOrderByRoleCodeAscSortOrderAscUpdatedAtDesc();

    Optional<OpsMenuPermissionProfile> findByRoleCodeAndMenuCode(String roleCode, String menuCode);
}
