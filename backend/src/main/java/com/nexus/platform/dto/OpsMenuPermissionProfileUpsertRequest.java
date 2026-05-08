package com.nexus.platform.dto;

public record OpsMenuPermissionProfileUpsertRequest(
        String roleCode,
        String menuCode,
        String menuLabel,
        Boolean enabled,
        Integer sortOrder,
        String note
) {
}
