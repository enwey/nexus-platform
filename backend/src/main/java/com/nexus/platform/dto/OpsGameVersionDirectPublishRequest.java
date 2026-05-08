package com.nexus.platform.dto;

public record OpsGameVersionDirectPublishRequest(
        String reason,
        Boolean forceUpdate
) {
}
