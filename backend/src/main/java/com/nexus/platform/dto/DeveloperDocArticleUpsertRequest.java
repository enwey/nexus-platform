package com.nexus.platform.dto;

import java.util.List;

public record DeveloperDocArticleUpsertRequest(
        String docType,
        String title,
        String category,
        String articleStatus,
        String summary,
        List<String> tags,
        String contentMarkdown,
        String changeNote
) {
}
