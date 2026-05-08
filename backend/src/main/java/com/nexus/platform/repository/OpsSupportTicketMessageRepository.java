package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsSupportTicketMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsSupportTicketMessageRepository extends JpaRepository<OpsSupportTicketMessage, Long> {
    List<OpsSupportTicketMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
