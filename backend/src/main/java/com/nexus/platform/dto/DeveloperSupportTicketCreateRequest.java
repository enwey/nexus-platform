package com.nexus.platform.dto;

public record DeveloperSupportTicketCreateRequest(
        String ticketType,
        String priority,
        String title,
        String content,
        String relatedAppId
) {
}
