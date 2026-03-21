package com.nexus.platform.dto;

import com.nexus.platform.entity.User;

public record UserProfileDto(
        Long id,
        String username,
        String email,
        String phone,
        String role
) {
    public static UserProfileDto from(User user) {
        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name()
        );
    }
}
