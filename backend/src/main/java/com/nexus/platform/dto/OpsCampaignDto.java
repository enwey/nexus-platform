package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsCampaignDto(
        Long id,
        String campaignCode,
        String campaignType,
        String title,
        String status,
        String effectiveStatus,
        String audience,
        String landingUrl,
        String bannerUrl,
        Integer priority,
        String note,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
