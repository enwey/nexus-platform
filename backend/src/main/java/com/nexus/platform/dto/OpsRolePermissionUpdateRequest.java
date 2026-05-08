package com.nexus.platform.dto;

import java.util.List;

public record OpsRolePermissionUpdateRequest(
        List<String> permissions
) {
}
