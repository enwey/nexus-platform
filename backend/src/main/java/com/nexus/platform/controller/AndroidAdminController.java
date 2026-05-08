package com.nexus.platform.controller;

import com.nexus.platform.dto.AndroidAdminDtos.AndroidBridgeApiItem;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidChannelRuleDto;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidChannelRuleUpsertRequest;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidAbExperimentDto;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidAbExperimentUpsertRequest;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidCompatibilityBlacklistDto;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidCompatibilityBlacklistUpsertRequest;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidConsolePayload;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidPageCircuitBreakerDto;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidPageCircuitBreakerUpsertRequest;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidFeatureToggleDto;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidFeatureToggleUpsertRequest;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidGameAssetRow;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidGrayReleasePlanDto;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidGrayReleasePlanUpsertRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
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
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        return Result.success(androidAdminService.updateRuntimeConfig(request, currentUser, httpRequest.getRequestURI()));
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

    @GetMapping("/channels")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<AndroidChannelRuleDto>> getChannels() {
        return Result.success(androidAdminService.getChannelRules());
    }

    @PostMapping("/channels")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<AndroidChannelRuleDto> upsertChannel(
            @RequestBody AndroidChannelRuleUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return Result.success(androidAdminService.upsertChannelRule(request, currentUser, httpRequest.getRequestURI()));
    }

    @GetMapping("/feature-toggles")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<AndroidFeatureToggleDto>> getFeatureToggles() {
        return Result.success(androidAdminService.getFeatureToggles());
    }

    @PostMapping("/feature-toggles")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<AndroidFeatureToggleDto> upsertFeatureToggle(
            @RequestBody AndroidFeatureToggleUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return Result.success(androidAdminService.upsertFeatureToggle(request, currentUser, httpRequest.getRequestURI()));
    }

    @GetMapping("/ab-experiments")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<AndroidAbExperimentDto>> getAbExperiments() {
        return Result.success(androidAdminService.getAbExperiments());
    }

    @PostMapping("/ab-experiments")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<AndroidAbExperimentDto> upsertAbExperiment(
            @RequestBody AndroidAbExperimentUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return Result.success(androidAdminService.upsertAbExperiment(request, currentUser, httpRequest.getRequestURI()));
    }

    @GetMapping("/gray-release-plans")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<AndroidGrayReleasePlanDto>> getGrayReleasePlans() {
        return Result.success(androidAdminService.getGrayReleasePlans());
    }

    @PostMapping("/gray-release-plans")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<AndroidGrayReleasePlanDto> upsertGrayReleasePlan(
            @RequestBody AndroidGrayReleasePlanUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return Result.success(androidAdminService.upsertGrayReleasePlan(request, currentUser, httpRequest.getRequestURI()));
    }

    @GetMapping("/compatibility-blacklists")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<AndroidCompatibilityBlacklistDto>> getCompatibilityBlacklists() {
        return Result.success(androidAdminService.getCompatibilityBlacklists());
    }

    @PostMapping("/compatibility-blacklists")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<AndroidCompatibilityBlacklistDto> upsertCompatibilityBlacklist(
            @RequestBody AndroidCompatibilityBlacklistUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return Result.success(androidAdminService.upsertCompatibilityBlacklist(request, currentUser, httpRequest.getRequestURI()));
    }

    @GetMapping("/page-circuit-breakers")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<AndroidPageCircuitBreakerDto>> getPageCircuitBreakers() {
        return Result.success(androidAdminService.getPageCircuitBreakers());
    }

    @PostMapping("/page-circuit-breakers")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<AndroidPageCircuitBreakerDto> upsertPageCircuitBreaker(
            @RequestBody AndroidPageCircuitBreakerUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return Result.success(androidAdminService.upsertPageCircuitBreaker(request, currentUser, httpRequest.getRequestURI()));
    }
}
