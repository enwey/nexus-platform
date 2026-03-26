package com.nexus.platform.dto;

public record AuthResponse(String token, String refreshToken, UserProfileDto user) {
}
