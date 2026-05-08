package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsGiftCodeCampaign;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsGiftCodeCampaignRepository extends JpaRepository<OpsGiftCodeCampaign, Long> {
    List<OpsGiftCodeCampaign> findAllByOrderByUpdatedAtDesc();
}
