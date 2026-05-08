package com.nexus.platform.dto;

public record OpsDiscoverPublishOrderStatusRequest(
        String status,
        String reason
) {
}
