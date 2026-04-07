package com.nexus.platform.controller;

import com.nexus.platform.dto.OpsDiscoverConfigResponse;
import com.nexus.platform.dto.OpsDiscoverConfigUpdateRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.OpsDiscoverAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ops/discover")
@RequiredArgsConstructor
public class OpsDiscoverAdminController {
    private final OpsDiscoverAdminService opsDiscoverAdminService;

    @GetMapping("/config")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<OpsDiscoverConfigResponse> getConfig() {
        return opsDiscoverAdminService.getConfig();
    }

    @PutMapping("/config")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsDiscoverConfigResponse> updateConfig(
            @RequestBody OpsDiscoverConfigUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return opsDiscoverAdminService.updateConfig(request, currentUser);
    }
}
