package com.nexus.platform.dto;

public record OpsRuleTemplateUpsertRequest(
        String templateType,
        String title,
        String content,
        Integer sortOrder
) {
}
