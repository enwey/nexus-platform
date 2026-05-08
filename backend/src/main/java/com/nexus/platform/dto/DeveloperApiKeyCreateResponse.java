package com.nexus.platform.dto;

public record DeveloperApiKeyCreateResponse(
        DeveloperApiKeyDto key,
        String secret
) {
}
