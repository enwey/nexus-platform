package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsCampaignUpsertRequest(
        String campaignType,
        String title,
        String status,
        String audience,
        String landingUrl,
        String bannerUrl,
        Integer priority,
        String note,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
