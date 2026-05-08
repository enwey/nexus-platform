package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsTaskCampaignDto(
        Long id,
        String taskCode,
        String title,
        String status,
        String effectiveStatus,
        String audience,
        String taskType,
        String rewardType,
        String rewardValue,
        String landingUrl,
        Integer priority,
        Integer dailyLimit,
        String note,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
