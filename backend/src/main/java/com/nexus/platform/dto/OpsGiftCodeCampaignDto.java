package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsGiftCodeCampaignDto(
        Long id,
        String campaignCode,
        String title,
        String status,
        String effectiveStatus,
        String audience,
        String rewardType,
        String rewardSummary,
        String landingUrl,
        String codePrefix,
        Integer totalStock,
        Integer availableStock,
        Integer redeemedStock,
        Integer perUserLimit,
        String note,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
