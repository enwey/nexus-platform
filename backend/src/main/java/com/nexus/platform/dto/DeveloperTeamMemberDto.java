package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record DeveloperTeamMemberDto(
        Long id,
        String memberName,
        String memberEmail,
        String teamRole,
        String memberStatus,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
