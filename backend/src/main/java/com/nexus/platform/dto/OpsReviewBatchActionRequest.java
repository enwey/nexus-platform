package com.nexus.platform.dto;

import java.util.List;

public record OpsReviewBatchActionRequest(
        List<Long> versionIds,
        String action,
        String reason,
        Long reviewerId,
        String note
) {
}
