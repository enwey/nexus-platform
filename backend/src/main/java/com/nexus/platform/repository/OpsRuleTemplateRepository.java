package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsRuleTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OpsRuleTemplateRepository extends JpaRepository<OpsRuleTemplate, Long> {
    List<OpsRuleTemplate> findByStatusOrderByTemplateTypeAscSortOrderAscUpdatedAtDesc(String status);
}
