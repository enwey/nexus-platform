package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DeveloperDocArticleDto(
        Long id,
        String docType,
        String title,
        String category,
        String articleStatus,
        String summary,
        List<String> tags,
        Integer currentVersion,
        String latestChangeNote,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
