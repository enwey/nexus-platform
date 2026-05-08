package com.nexus.platform.dto;

import java.util.List;

public record OpsMarketingBatchStatusRequest(
        String assetType,
        List<Long> ids,
        String status,
        String reason
) {
}
