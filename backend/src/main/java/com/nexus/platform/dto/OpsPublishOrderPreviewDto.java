package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record OpsPublishOrderPreviewDto(
        Long orderId,
        String assetType,
        String assetCode,
        String assetName,
        String desiredAction,
        String rolloutMode,
        Integer rolloutPercent,
        String rolloutChannel,
        String orderStatus,
        LocalDateTime scheduleAt,
        Map<String, Object> submittedSnapshot,
        Map<String, Object> currentSnapshot,
        Map<String, Object> desiredSnapshot,
        List<FieldDiff> fieldDiffs,
        List<FieldDiff> configDriftDiffs,
        List<String> riskWarnings,
        Map<String, Object> executionBeforeSnapshot,
        Map<String, Object> executionAfterSnapshot,
        List<FieldDiff> executionDiffs,
        String executionResult,
        String executionReceipt,
        String failureReason,
        Integer retryCount
) {
    public record FieldDiff(
            String field,
            String beforeValue,
            String afterValue
    ) {
    }
}
