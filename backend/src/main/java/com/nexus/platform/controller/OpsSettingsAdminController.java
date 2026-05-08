package com.nexus.platform.controller;

import com.nexus.platform.dto.OpsAdminDataScopeDto;
import com.nexus.platform.dto.OpsAdminDataScopeUpdateRequest;
import com.nexus.platform.dto.OpsApprovalTemplateDto;
import com.nexus.platform.dto.OpsApprovalTemplateUpsertRequest;
import com.nexus.platform.dto.OpsDictionaryEntryDto;
import com.nexus.platform.dto.OpsDictionaryEntryUpsertRequest;
import com.nexus.platform.dto.OpsMenuPermissionProfileDto;
import com.nexus.platform.dto.OpsMenuPermissionProfileUpsertRequest;
import com.nexus.platform.dto.OpsNoticeTemplateDto;
import com.nexus.platform.dto.OpsNoticeTemplateUpsertRequest;
import com.nexus.platform.dto.OpsPermissionMatrixDto;
import com.nexus.platform.dto.OpsRolePermissionUpdateRequest;
import com.nexus.platform.dto.OpsSensitivePolicyDto;
import com.nexus.platform.dto.OpsSensitivePolicyUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.OpsSettingsAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ops/settings")
@RequiredArgsConstructor
public class OpsSettingsAdminController {
    private final OpsSettingsAdminService opsSettingsAdminService;

    @GetMapping("/permission-matrix")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<OpsPermissionMatrixDto> permissionMatrix() {
        return Result.success(opsSettingsAdminService.getPermissionMatrix());
    }

    @GetMapping("/admin-data-scopes")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<java.util.List<OpsAdminDataScopeDto>> adminDataScopes() {
        return Result.success(opsSettingsAdminService.listAdminDataScopes());
    }

    @PutMapping("/permission-matrix/{roleCode}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsPermissionMatrixDto.RolePermissionItem> updatePermissionMatrix(
            @PathVariable String roleCode,
            @RequestBody OpsRolePermissionUpdateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return opsSettingsAdminService.updateRolePermissions(roleCode, request, currentUser);
    }

    @PutMapping("/admin-data-scopes/{adminUserId}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsAdminDataScopeDto> updateAdminDataScope(
            @PathVariable Long adminUserId,
            @RequestBody OpsAdminDataScopeUpdateRequest request
    ) {
        return opsSettingsAdminService.updateAdminDataScope(adminUserId, request);
    }

    @GetMapping("/notice-templates")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<java.util.List<OpsNoticeTemplateDto>> noticeTemplates() {
        return Result.success(opsSettingsAdminService.listNoticeTemplates());
    }

    @PutMapping("/notice-templates")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsNoticeTemplateDto> upsertNoticeTemplate(
            @RequestBody OpsNoticeTemplateUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsSettingsAdminService.upsertNoticeTemplate(null, request, currentUser, httpRequest.getRequestURI());
    }

    @PutMapping("/notice-templates/{id}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsNoticeTemplateDto> updateNoticeTemplate(
            @PathVariable Long id,
            @RequestBody OpsNoticeTemplateUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsSettingsAdminService.upsertNoticeTemplate(id, request, currentUser, httpRequest.getRequestURI());
    }

    @GetMapping("/dictionary-entries")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<java.util.List<OpsDictionaryEntryDto>> dictionaryEntries() {
        return Result.success(opsSettingsAdminService.listDictionaryEntries());
    }

    @PutMapping("/dictionary-entries")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsDictionaryEntryDto> upsertDictionaryEntry(
            @RequestBody OpsDictionaryEntryUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsSettingsAdminService.upsertDictionaryEntry(null, request, currentUser, httpRequest.getRequestURI());
    }

    @PutMapping("/dictionary-entries/{id}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsDictionaryEntryDto> updateDictionaryEntry(
            @PathVariable Long id,
            @RequestBody OpsDictionaryEntryUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsSettingsAdminService.upsertDictionaryEntry(id, request, currentUser, httpRequest.getRequestURI());
    }

    @GetMapping("/menu-permissions")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<java.util.List<OpsMenuPermissionProfileDto>> menuPermissions() {
        return Result.success(opsSettingsAdminService.listMenuPermissionProfiles());
    }

    @PutMapping("/menu-permissions")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsMenuPermissionProfileDto> upsertMenuPermission(
            @RequestBody OpsMenuPermissionProfileUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsSettingsAdminService.upsertMenuPermissionProfile(null, request, currentUser, httpRequest.getRequestURI());
    }

    @PutMapping("/menu-permissions/{id}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsMenuPermissionProfileDto> updateMenuPermission(
            @PathVariable Long id,
            @RequestBody OpsMenuPermissionProfileUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsSettingsAdminService.upsertMenuPermissionProfile(id, request, currentUser, httpRequest.getRequestURI());
    }

    @GetMapping("/approval-templates")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<java.util.List<OpsApprovalTemplateDto>> approvalTemplates() {
        return Result.success(opsSettingsAdminService.listApprovalTemplates());
    }

    @PutMapping("/approval-templates")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsApprovalTemplateDto> upsertApprovalTemplate(
            @RequestBody OpsApprovalTemplateUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsSettingsAdminService.upsertApprovalTemplate(null, request, currentUser, httpRequest.getRequestURI());
    }

    @PutMapping("/approval-templates/{id}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsApprovalTemplateDto> updateApprovalTemplate(
            @PathVariable Long id,
            @RequestBody OpsApprovalTemplateUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsSettingsAdminService.upsertApprovalTemplate(id, request, currentUser, httpRequest.getRequestURI());
    }

    @GetMapping("/sensitive-policies")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<java.util.List<OpsSensitivePolicyDto>> sensitivePolicies() {
        return Result.success(opsSettingsAdminService.listSensitivePolicies());
    }

    @PutMapping("/sensitive-policies")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsSensitivePolicyDto> upsertSensitivePolicy(
            @RequestBody OpsSensitivePolicyUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsSettingsAdminService.upsertSensitivePolicy(null, request, currentUser, httpRequest.getRequestURI());
    }

    @PutMapping("/sensitive-policies/{id}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsSensitivePolicyDto> updateSensitivePolicy(
            @PathVariable Long id,
            @RequestBody OpsSensitivePolicyUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsSettingsAdminService.upsertSensitivePolicy(id, request, currentUser, httpRequest.getRequestURI());
    }
}
