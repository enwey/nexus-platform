package com.nexus.platform.controller;

import com.nexus.platform.dto.OpsAccessControlRuleDto;
import com.nexus.platform.dto.OpsAccessControlRuleUpsertRequest;
import com.nexus.platform.dto.OpsLoginRiskEventDto;
import com.nexus.platform.dto.OpsRiskRuleConfigDto;
import com.nexus.platform.dto.OpsRiskRuleConfigUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.LoginRiskOpsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ops/login-risk")
@RequiredArgsConstructor
public class OpsLoginRiskAdminController {
    private final LoginRiskOpsService loginRiskOpsService;

    @GetMapping("/events")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).AUDIT_LOG_READ)")
    public Result<List<OpsLoginRiskEventDto>> listEvents() {
        return loginRiskOpsService.listEvents();
    }

    @GetMapping("/rules")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).AUDIT_LOG_READ)")
    public Result<List<OpsAccessControlRuleDto>> listRules() {
        return loginRiskOpsService.listRules();
    }

    @GetMapping("/risk-rules")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).AUDIT_LOG_READ)")
    public Result<List<OpsRiskRuleConfigDto>> listRiskRules() {
        return loginRiskOpsService.listRiskRules();
    }

    @PostMapping("/rules")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsAccessControlRuleDto> upsertRule(
            @RequestBody OpsAccessControlRuleUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return loginRiskOpsService.upsertRule(request, currentUser, httpRequest.getRequestURI());
    }

    @PostMapping("/risk-rules")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsRiskRuleConfigDto> upsertRiskRule(
            @RequestBody OpsRiskRuleConfigUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return loginRiskOpsService.upsertRiskRule(request, currentUser, httpRequest.getRequestURI());
    }
}
