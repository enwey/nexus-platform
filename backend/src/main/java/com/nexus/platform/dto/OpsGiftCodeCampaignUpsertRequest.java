package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsGiftCodeCampaignUpsertRequest(
        String title,
        String status,
        String audience,
        String rewardType,
        String rewardSummary,
        String landingUrl,
        String codePrefix,
        Integer totalStock,
        Integer perUserLimit,
        String note,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
