package com.nexus.platform.dto;

public record UserProfileDetailDto(
        Long id,
        String username,
        String email,
        String phone,
        String role,
        String displayName,
        String avatarUrl,
        String languageTag
) {
}
