package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsSupportTicket;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsSupportTicketRepository extends JpaRepository<OpsSupportTicket, Long> {
    List<OpsSupportTicket> findByDeveloperIdOrderByUpdatedAtDesc(Long developerId);
    Optional<OpsSupportTicket> findByIdAndDeveloperId(Long id, Long developerId);
}
