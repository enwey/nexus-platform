package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsPopupCampaignDto(
        Long id,
        String popupCode,
        String title,
        String status,
        String effectiveStatus,
        String audience,
        String triggerScene,
        String landingUrl,
        String imageUrl,
        String buttonText,
        Integer priority,
        Integer frequencyLimitPerDay,
        String note,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
