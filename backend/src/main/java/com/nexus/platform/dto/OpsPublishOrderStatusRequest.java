package com.nexus.platform.dto;

public record OpsPublishOrderStatusRequest(
        String orderStatus,
        String reason
) {
}
