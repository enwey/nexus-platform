package com.nexus.platform.dto;

import java.util.List;

public record OpsDiscoverBatchStatusRequest(
        List<Long> itemIds,
        String status
) {
}
