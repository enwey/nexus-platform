package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsCampaign;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsCampaignRepository extends JpaRepository<OpsCampaign, Long> {
    List<OpsCampaign> findAllByOrderByUpdatedAtDesc();
}
