package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record DeveloperDocArticleVersionDto(
        Long id,
        Long articleId,
        Integer versionNo,
        String titleSnapshot,
        String contentMarkdown,
        String changeNote,
        Long createdBy,
        LocalDateTime createdAt
) {
}
