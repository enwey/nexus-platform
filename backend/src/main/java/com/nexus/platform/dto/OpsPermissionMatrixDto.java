package com.nexus.platform.dto;

import java.util.List;

public record OpsPermissionMatrixDto(
        List<String> availablePermissions,
        List<RolePermissionItem> roles
) {
    public record RolePermissionItem(
            String role,
            List<String> permissions,
            boolean editable
    ) {
    }
}
