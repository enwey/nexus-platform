package com.nexus.platform.controller;

import com.nexus.platform.dto.AndroidAdminDtos.AndroidBridgeApiItem;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidConsolePayload;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidGameAssetRow;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidRuntimeConfig;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidRuntimeConfigUpdateRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.AndroidAdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/android")
@RequiredArgsConstructor
public class AndroidAdminController {
    private final AndroidAdminService androidAdminService;

    @GetMapping("/console")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<AndroidConsolePayload> getConsole() {
        return Result.success(androidAdminService.getConsolePayload());
    }

    @GetMapping("/config")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<AndroidRuntimeConfig> getConfig() {
        return Result.success(androidAdminService.getRuntimeConfig());
    }

    @PutMapping("/config")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<AndroidRuntimeConfig> updateConfig(
            @RequestBody AndroidRuntimeConfigUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return Result.success(androidAdminService.updateRuntimeConfig(request, currentUser));
    }

    @GetMapping("/bridge/apis")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<AndroidBridgeApiItem>> getBridgeApis() {
        return Result.success(androidAdminService.getBridgeApis());
    }

    @GetMapping("/games")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<AndroidGameAssetRow>> getGames() {
        return Result.success(androidAdminService.getGameAssets());
    }
}
