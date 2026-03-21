package com.nexus.platform.dto;

public record AuthResponse(String token, UserProfileDto user) {
}
