package com.nexus.platform.repository;

import com.nexus.platform.entity.GameMediaAsset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameMediaAssetRepository extends JpaRepository<GameMediaAsset, Long> {
    List<GameMediaAsset> findByGameIdAndMediaTypeOrderByPrimaryDescSortOrderAsc(Long gameId, String mediaType);

    List<GameMediaAsset> findByGameIdOrderByAssetGroupAscSortOrderAscPrimaryDesc(Long gameId);

    List<GameMediaAsset> findByGameIdAndAssetGroupOrderBySortOrderAscPrimaryDesc(Long gameId, String assetGroup);

    Optional<GameMediaAsset> findByIdAndGameId(Long id, Long gameId);

    List<GameMediaAsset> findByGameIdAndAssetGroupAndAssetRole(Long gameId, String assetGroup, String assetRole);

    void deleteByIdIn(Collection<Long> ids);
}
