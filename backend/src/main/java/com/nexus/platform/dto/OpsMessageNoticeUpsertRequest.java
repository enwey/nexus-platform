package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsMessageNoticeUpsertRequest(
        String audienceRole,
        Long targetUserId,
        String category,
        String deliveryStatus,
        String title,
        String body,
        String actionUrl,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
