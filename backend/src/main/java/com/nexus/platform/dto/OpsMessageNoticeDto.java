package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsMessageNoticeDto(
        Long id,
        String audienceRole,
        Long targetUserId,
        String category,
        String deliveryStatus,
        String effectiveStatus,
        String title,
        String body,
        String actionUrl,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
