package com.nexus.platform.controller;

import com.nexus.platform.config.AuthInterceptor;
import com.nexus.platform.dto.OpsDiscoverConfigResponse;
import com.nexus.platform.dto.OpsDiscoverConfigUpdateRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.OpsDiscoverAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ops/discover")
@RequiredArgsConstructor
public class OpsDiscoverAdminController {
    private final OpsDiscoverAdminService opsDiscoverAdminService;

    @GetMapping("/config")
    public Result<OpsDiscoverConfigResponse> getConfig() {
        return opsDiscoverAdminService.getConfig();
    }

    @PutMapping("/config")
    public Result<OpsDiscoverConfigResponse> updateConfig(
            @RequestBody OpsDiscoverConfigUpdateRequest request,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User currentUser) {
        return opsDiscoverAdminService.updateConfig(request, currentUser);
    }
}
