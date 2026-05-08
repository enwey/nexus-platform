package com.nexus.platform.dto;

public record DeveloperTeamMemberUpsertRequest(
        String memberName,
        String memberEmail,
        String teamRole,
        String memberStatus,
        String note
) {
}
