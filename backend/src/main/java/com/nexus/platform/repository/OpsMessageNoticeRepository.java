package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsMessageNotice;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsMessageNoticeRepository extends JpaRepository<OpsMessageNotice, Long> {
    List<OpsMessageNotice> findAllByOrderByUpdatedAtDesc();

    List<OpsMessageNotice> findByDeliveryStatusAndAudienceRoleInAndStartAtBeforeOrStartAtIsNullOrderByUpdatedAtDesc(
            String deliveryStatus,
            List<String> audienceRoles,
            LocalDateTime startAt
    );
}
