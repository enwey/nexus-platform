package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record DeveloperUploadTaskDto(
        Long gameId,
        String appId,
        String gameName,
        String originalFilename,
        Long fileSizeBytes,
        String taskStatus,
        String gameStatus,
        String latestVersion,
        String failureReason,
        Integer retryCount,
        Boolean manifestValid,
        String manifestSummary,
        LocalDateTime processingStartedAt,
        LocalDateTime processingFinishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
