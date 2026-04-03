package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsCollectionGameRel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OpsCollectionGameRelRepository extends JpaRepository<OpsCollectionGameRel, Long> {
    List<OpsCollectionGameRel> findByCollectionId(Long collectionId);

    List<OpsCollectionGameRel> findByCollectionIdOrderByPinTopDescSortOrderAscUpdatedAtDesc(Long collectionId);

    void deleteByCollectionId(Long collectionId);
}
