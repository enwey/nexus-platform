package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsPublishOrder;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsPublishOrderRepository extends JpaRepository<OpsPublishOrder, Long> {
    List<OpsPublishOrder> findAllByOrderByUpdatedAtDesc();

    List<OpsPublishOrder> findByOrderStatusAndScheduleAtLessThanEqualOrderByScheduleAtAsc(
            String orderStatus,
            LocalDateTime scheduleAt
    );
}
