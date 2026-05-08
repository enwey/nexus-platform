package com.nexus.platform.dto;

public record OpsNoticeTemplateUpsertRequest(
        String templateCode,
        String templateName,
        String channelType,
        String languageTag,
        String titleTemplate,
        String bodyTemplate,
        String status
) {
}
