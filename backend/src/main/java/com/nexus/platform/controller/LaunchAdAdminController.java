package com.nexus.platform.controller;

import com.nexus.platform.dto.LaunchAdDtos.LaunchAdAdminItem;
import com.nexus.platform.dto.LaunchAdDtos.LaunchAdAdminListResponse;
import com.nexus.platform.dto.LaunchAdDtos.LaunchAdUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.LaunchAdService;
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
@RequestMapping("/admin/launch-ads")
@RequiredArgsConstructor
public class LaunchAdAdminController {
    private final LaunchAdService launchAdService;

    @GetMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<LaunchAdAdminListResponse> list() {
        return launchAdService.listAdminAds();
    }

    @PostMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<LaunchAdAdminItem> create(
            @RequestBody LaunchAdUpsertRequest request,
            @AuthenticationPrincipal User currentUser) {
        return launchAdService.create(request, currentUser);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<LaunchAdAdminItem> update(
            @PathVariable Long id,
            @RequestBody LaunchAdUpsertRequest request,
            @AuthenticationPrincipal User currentUser) {
        return launchAdService.update(id, request, currentUser);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<LaunchAdAdminItem> activate(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return launchAdService.activate(id, currentUser);
    }
}
