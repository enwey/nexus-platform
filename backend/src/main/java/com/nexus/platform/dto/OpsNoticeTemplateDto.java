package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsNoticeTemplateDto(
        Long id,
        String templateCode,
        String templateName,
        String channelType,
        String languageTag,
        String titleTemplate,
        String bodyTemplate,
        String status,
        String updatedBy,
        LocalDateTime updatedAt
) {
}
