package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsCollection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OpsCollectionRepository extends JpaRepository<OpsCollection, Long> {
    Optional<OpsCollection> findByCollectionCodeAndStatus(String collectionCode, String status);
}
