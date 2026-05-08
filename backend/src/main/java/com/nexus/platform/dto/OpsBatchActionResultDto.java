package com.nexus.platform.dto;

import java.util.List;

public record OpsBatchActionResultDto(
        int totalRequested,
        int totalSucceeded,
        String action,
        List<Long> affectedIds
) {
}
