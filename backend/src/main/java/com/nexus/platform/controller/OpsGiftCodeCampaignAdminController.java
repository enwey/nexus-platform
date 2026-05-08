package com.nexus.platform.controller;

import com.nexus.platform.dto.OpsGiftCodeCampaignDto;
import com.nexus.platform.dto.OpsGiftCodeCampaignStatusRequest;
import com.nexus.platform.dto.OpsGiftCodeCampaignUpsertRequest;
import com.nexus.platform.dto.OpsGiftCodeEntryDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.OpsGiftCodeCampaignService;
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
@RequestMapping("/admin/ops/gift-code-campaigns")
@RequiredArgsConstructor
public class OpsGiftCodeCampaignAdminController {
    private final OpsGiftCodeCampaignService service;

    @GetMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<OpsGiftCodeCampaignDto>> list() {
        return service.list();
    }

    @GetMapping("/{id}/codes")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<OpsGiftCodeEntryDto>> listCodes(@PathVariable Long id) {
        return service.listCodes(id);
    }

    @PostMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsGiftCodeCampaignDto> create(
            @RequestBody OpsGiftCodeCampaignUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest servletRequest
    ) {
        return service.create(request, currentUser, servletRequest.getRequestURI());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsGiftCodeCampaignDto> update(
            @PathVariable Long id,
            @RequestBody OpsGiftCodeCampaignUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest servletRequest
    ) {
        return service.update(id, request, currentUser, servletRequest.getRequestURI());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsGiftCodeCampaignDto> updateStatus(
            @PathVariable Long id,
            @RequestBody OpsGiftCodeCampaignStatusRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest servletRequest
    ) {
        return service.updateStatus(id, request, currentUser, servletRequest.getRequestURI());
    }
}
