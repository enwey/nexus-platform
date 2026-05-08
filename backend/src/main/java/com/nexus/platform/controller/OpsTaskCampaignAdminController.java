package com.nexus.platform.controller;

import com.nexus.platform.dto.OpsTaskCampaignDto;
import com.nexus.platform.dto.OpsTaskCampaignStatusRequest;
import com.nexus.platform.dto.OpsTaskCampaignUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.OpsTaskCampaignService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ops/task-campaigns")
@RequiredArgsConstructor
public class OpsTaskCampaignAdminController {
    private final OpsTaskCampaignService service;

    @GetMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<OpsTaskCampaignDto>> list() {
        return service.list();
    }

    @PostMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsTaskCampaignDto> create(
            @RequestBody OpsTaskCampaignUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return service.create(request, currentUser, httpRequest.getRequestURI());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsTaskCampaignDto> update(
            @PathVariable Long id,
            @RequestBody OpsTaskCampaignUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return service.update(id, request, currentUser, httpRequest.getRequestURI());
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsTaskCampaignDto> updateStatus(
            @PathVariable Long id,
            @RequestBody OpsTaskCampaignStatusRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return service.updateStatus(id, request, currentUser, httpRequest.getRequestURI());
    }
}
