package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OpsDeveloperAccountDto(
        Long id,
        String username,
        String email,
        String role,
        String accountStatus,
        String certificationStatus,
        String certificationProfileStatus,
        String certificationSubjectType,
        String certificationSubjectName,
        String riskLevel,
        String whitelistStatus,
        Integer violationCount,
        String governanceTag,
        String opsNote,
        LocalDateTime createdAt,
        int gameCount,
        int approvedGames,
        int pendingGames,
        int rejectedGames,
        LocalDateTime lastGameAt,
        List<String> games
) {
}
