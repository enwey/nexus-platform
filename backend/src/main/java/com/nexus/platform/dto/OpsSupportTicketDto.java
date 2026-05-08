package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsSupportTicketDto(
        Long id,
        String ticketNo,
        Long developerId,
        String developerName,
        String developerEmail,
        String ticketType,
        String priority,
        String title,
        String content,
        String ticketStatus,
        String relatedAppId,
        Long assigneeAdminId,
        String resolutionSummary,
        LocalDateTime lastReplyAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
