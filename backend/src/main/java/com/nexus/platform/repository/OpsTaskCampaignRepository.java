package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsTaskCampaign;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsTaskCampaignRepository extends JpaRepository<OpsTaskCampaign, Long> {
    List<OpsTaskCampaign> findAllByOrderByUpdatedAtDesc();

    Optional<OpsTaskCampaign> findByTaskCode(String taskCode);
}
