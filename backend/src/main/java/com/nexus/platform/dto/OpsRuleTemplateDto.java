package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsRuleTemplateDto(
        Long id,
        String templateType,
        String title,
        String content,
        String status,
        Integer sortOrder,
        LocalDateTime updatedAt
) {
}
