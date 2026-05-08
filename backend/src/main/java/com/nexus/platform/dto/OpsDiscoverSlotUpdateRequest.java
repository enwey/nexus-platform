package com.nexus.platform.dto;

public record OpsDiscoverSlotUpdateRequest(
        Boolean enabled,
        String reason
) {
}
