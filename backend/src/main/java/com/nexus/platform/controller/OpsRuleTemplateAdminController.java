package com.nexus.platform.controller;

import com.nexus.platform.dto.OpsRuleTemplateDto;
import com.nexus.platform.dto.OpsRuleTemplateUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.OpsRuleTemplateService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ops/settings/rule-templates")
@RequiredArgsConstructor
public class OpsRuleTemplateAdminController {
    private final OpsRuleTemplateService opsRuleTemplateService;

    @GetMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<OpsRuleTemplateDto>> list() {
        return opsRuleTemplateService.list();
    }

    @PostMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<List<OpsRuleTemplateDto>> create(
            @RequestBody OpsRuleTemplateUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        return opsRuleTemplateService.create(request, currentUser, httpRequest.getRequestURI());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<List<OpsRuleTemplateDto>> update(
            @PathVariable Long id,
            @RequestBody OpsRuleTemplateUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        return opsRuleTemplateService.update(id, request, currentUser, httpRequest.getRequestURI());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<List<OpsRuleTemplateDto>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        return opsRuleTemplateService.delete(id, currentUser, httpRequest.getRequestURI());
    }
}
