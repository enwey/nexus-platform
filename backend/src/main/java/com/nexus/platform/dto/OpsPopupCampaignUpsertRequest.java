package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsPopupCampaignUpsertRequest(
        String title,
        String status,
        String audience,
        String triggerScene,
        String landingUrl,
        String imageUrl,
        String buttonText,
        Integer priority,
        Integer frequencyLimitPerDay,
        String note,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
