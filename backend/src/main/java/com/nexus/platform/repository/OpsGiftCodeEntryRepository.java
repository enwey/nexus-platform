package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsGiftCodeCampaign;
import com.nexus.platform.entity.OpsGiftCodeEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsGiftCodeEntryRepository extends JpaRepository<OpsGiftCodeEntry, Long> {
    boolean existsByCode(String code);

    long countByCampaignIdAndStatus(Long campaignId, String status);

    long countByCampaign(OpsGiftCodeCampaign campaign);

    List<OpsGiftCodeEntry> findTop200ByCampaignIdOrderByCreatedAtDesc(Long campaignId);
}
