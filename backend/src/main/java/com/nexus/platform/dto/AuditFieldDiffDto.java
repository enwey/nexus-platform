package com.nexus.platform.dto;

public record AuditFieldDiffDto(
        String field,
        String beforeValue,
        String afterValue,
        String changeType
) {
}
