package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsContentItem;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OpsContentItemRepository extends JpaRepository<OpsContentItem, Long> {
    @Query("""
            SELECT i FROM OpsContentItem i
            WHERE i.slotId = :slotId
              AND i.status = 'PUBLISHED'
              AND (i.startAt IS NULL OR i.startAt <= :now)
              AND (i.endAt IS NULL OR i.endAt >= :now)
            ORDER BY i.sortOrder ASC, i.updatedAt DESC
            """)
    List<OpsContentItem> findActiveBySlotId(@Param("slotId") Long slotId, @Param("now") LocalDateTime now);

    List<OpsContentItem> findBySlotIdOrderBySortOrderAscUpdatedAtDesc(Long slotId);

    void deleteBySlotId(Long slotId);

    long countBySlotIdAndBadgeText(Long slotId, String badgeText);

    @Modifying
    @Query("""
            UPDATE OpsContentItem i
            SET i.badgeText = :newBadgeText
            WHERE i.slotId = :slotId
              AND i.badgeText = :oldBadgeText
            """)
    int updateBadgeTextBySlotIdAndBadgeText(
            @Param("slotId") Long slotId,
            @Param("oldBadgeText") String oldBadgeText,
            @Param("newBadgeText") String newBadgeText
    );
}
