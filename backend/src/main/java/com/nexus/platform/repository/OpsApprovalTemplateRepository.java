package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsApprovalTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsApprovalTemplateRepository extends JpaRepository<OpsApprovalTemplate, Long> {
    List<OpsApprovalTemplate> findAllByOrderByBizTypeAscUpdatedAtDesc();

    Optional<OpsApprovalTemplate> findByTemplateCode(String templateCode);
}
