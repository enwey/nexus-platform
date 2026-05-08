package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsPublishOrderDto(
        Long id,
        String assetType,
        Long targetId,
        String assetCode,
        String assetName,
        String currentStatus,
        String desiredAction,
        String rolloutMode,
        Integer rolloutPercent,
        String rolloutChannel,
        String orderStatus,
        LocalDateTime scheduleAt,
        String executionNote,
        String rejectReason,
        String executionResult,
        String failureReason,
        String executionReceipt,
        Integer retryCount,
        LocalDateTime lastAttemptAt,
        Long submittedBy,
        Long approvedBy,
        Long publishedBy,
        Long cancelledBy,
        LocalDateTime submittedAt,
        LocalDateTime approvedAt,
        LocalDateTime publishedAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
