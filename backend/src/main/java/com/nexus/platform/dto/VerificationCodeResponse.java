package com.nexus.platform.dto;

public record VerificationCodeResponse(
        String account,
        String purpose,
        int expiresInSeconds,
        String debugCode
) {
}
