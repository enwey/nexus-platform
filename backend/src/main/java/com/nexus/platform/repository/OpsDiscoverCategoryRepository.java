package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsDiscoverCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OpsDiscoverCategoryRepository extends JpaRepository<OpsDiscoverCategory, Long> {
    List<OpsDiscoverCategory> findByStatusOrderBySortOrderAscUpdatedAtDesc(String status);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Optional<OpsDiscoverCategory> findByNameIgnoreCaseAndStatus(String name, String status);
}
