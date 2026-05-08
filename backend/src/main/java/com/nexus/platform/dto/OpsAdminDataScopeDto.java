package com.nexus.platform.dto;

public record OpsAdminDataScopeDto(
        Long adminUserId,
        String username,
        String email,
        String scopeCode
) {
}
