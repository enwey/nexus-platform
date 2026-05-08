package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsTaskCampaignUpsertRequest(
        String title,
        String status,
        String audience,
        String taskType,
        String rewardType,
        String rewardValue,
        String landingUrl,
        Integer priority,
        Integer dailyLimit,
        String note,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
