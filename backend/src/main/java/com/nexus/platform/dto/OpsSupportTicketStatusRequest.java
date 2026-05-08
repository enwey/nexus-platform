package com.nexus.platform.dto;

public record OpsSupportTicketStatusRequest(
        String ticketStatus,
        Long assigneeAdminId,
        String resolutionSummary
) {
}
