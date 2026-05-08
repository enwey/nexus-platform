package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsSupportTicketMessageDto(
        Long id,
        Long ticketId,
        Long senderId,
        String senderRole,
        String messageType,
        String content,
        LocalDateTime createdAt
) {
}
