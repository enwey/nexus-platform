package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsGameCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OpsGameCategoryRepository extends JpaRepository<OpsGameCategory, Long> {
    List<OpsGameCategory> findByStatusOrderBySortOrderAscUpdatedAtDesc(String status);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
