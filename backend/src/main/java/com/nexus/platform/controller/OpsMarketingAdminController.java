package com.nexus.platform.controller;

import com.nexus.platform.dto.OpsBatchActionResultDto;
import com.nexus.platform.dto.OpsMarketingAnalyticsDto;
import com.nexus.platform.dto.OpsMarketingBatchStatusRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.OpsMarketingAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ops/marketing")
@RequiredArgsConstructor
public class OpsMarketingAdminController {
    private final OpsMarketingAdminService opsMarketingAdminService;

    @GetMapping("/analytics")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<OpsMarketingAnalyticsDto> getAnalytics() {
        return opsMarketingAdminService.getAnalytics();
    }

    @PostMapping("/batch-status")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsBatchActionResultDto> batchUpdateStatus(
            @RequestBody OpsMarketingBatchStatusRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsMarketingAdminService.batchUpdateStatus(request, currentUser, httpRequest.getRequestURI());
    }
}
