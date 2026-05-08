package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DeveloperApiKeyCreateRequest(
        String keyName,
        List<String> scopes,
        LocalDateTime expiresAt
) {
}
