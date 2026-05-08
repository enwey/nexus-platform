package com.nexus.platform.controller;

import com.nexus.platform.dto.OpsPublishOverviewDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.service.OpsPublishAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ops/publish")
@RequiredArgsConstructor
public class OpsPublishAdminController {
    private final OpsPublishAdminService opsPublishAdminService;

    @GetMapping("/overview")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<OpsPublishOverviewDto> overview() {
        return opsPublishAdminService.getOverview();
    }
}
