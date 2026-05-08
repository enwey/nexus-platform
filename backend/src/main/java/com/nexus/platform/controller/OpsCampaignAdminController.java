package com.nexus.platform.controller;

import com.nexus.platform.dto.OpsCampaignDto;
import com.nexus.platform.dto.OpsCampaignStatusRequest;
import com.nexus.platform.dto.OpsCampaignUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.OpsCampaignService;
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
@RequestMapping("/admin/ops/campaigns")
@RequiredArgsConstructor
public class OpsCampaignAdminController {
    private final OpsCampaignService opsCampaignService;

    @GetMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<OpsCampaignDto>> list() {
        return opsCampaignService.listCampaigns();
    }

    @PostMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsCampaignDto> create(
            @RequestBody OpsCampaignUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsCampaignService.create(request, currentUser, httpRequest.getRequestURI());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsCampaignDto> update(
            @PathVariable Long id,
            @RequestBody OpsCampaignUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsCampaignService.update(id, request, currentUser, httpRequest.getRequestURI());
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsCampaignDto> updateStatus(
            @PathVariable Long id,
            @RequestBody OpsCampaignStatusRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsCampaignService.updateStatus(id, request, currentUser, httpRequest.getRequestURI());
    }
}
