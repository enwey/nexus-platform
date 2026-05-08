package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsPopupCampaign;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsPopupCampaignRepository extends JpaRepository<OpsPopupCampaign, Long> {
    List<OpsPopupCampaign> findAllByOrderByUpdatedAtDesc();

    Optional<OpsPopupCampaign> findByPopupCode(String popupCode);
}
