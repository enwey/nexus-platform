package com.nexus.platform.repository;

import com.nexus.platform.entity.GameMediaAsset;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameMediaAssetRepository extends JpaRepository<GameMediaAsset, Long> {
    List<GameMediaAsset> findByGameIdAndMediaTypeOrderByPrimaryDescSortOrderAsc(Long gameId, String mediaType);
}
