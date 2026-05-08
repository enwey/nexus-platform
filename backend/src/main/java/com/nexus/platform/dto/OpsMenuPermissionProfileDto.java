package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsMenuPermissionProfileDto(
        Long id,
        String roleCode,
        String menuCode,
        String menuLabel,
        Boolean enabled,
        Integer sortOrder,
        String note,
        String updatedBy,
        LocalDateTime updatedAt
) {
}
