package com.nexus.platform.dto;

import java.util.List;

public record OpsDiscoverBatchOperationRequest(
        String targetType,
        List<Long> ids,
        String status,
        String reason
) {
}
