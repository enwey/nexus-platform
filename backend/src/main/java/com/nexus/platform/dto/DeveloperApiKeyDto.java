package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DeveloperApiKeyDto(
        Long id,
        String keyName,
        String accessKey,
        List<String> scopes,
        String status,
        LocalDateTime expiresAt,
        LocalDateTime lastUsedAt,
        LocalDateTime revokedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
