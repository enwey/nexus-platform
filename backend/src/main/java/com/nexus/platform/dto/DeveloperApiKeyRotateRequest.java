package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DeveloperApiKeyRotateRequest(
        String confirmText,
        String newKeyName,
        List<String> scopes,
        LocalDateTime expiresAt
) {
}
