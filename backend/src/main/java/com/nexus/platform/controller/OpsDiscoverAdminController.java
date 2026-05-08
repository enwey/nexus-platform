package com.nexus.platform.controller;

import com.nexus.platform.dto.OpsDiscoverCategoryRequest;
import com.nexus.platform.dto.OpsDiscoverCategoryResponse;
import com.nexus.platform.dto.OpsDiscoverBatchStatusRequest;
import com.nexus.platform.dto.OpsDiscoverBatchOperationRequest;
import com.nexus.platform.dto.OpsDiscoverAnalyticsDto;
import com.nexus.platform.dto.OpsDiscoverConfigResponse;
import com.nexus.platform.dto.OpsDiscoverConfigUpdateRequest;
import com.nexus.platform.dto.OpsDiscoverExperimentRequest;
import com.nexus.platform.dto.OpsDiscoverExperimentStatusRequest;
import com.nexus.platform.dto.OpsDiscoverPublishOrderCreateRequest;
import com.nexus.platform.dto.OpsDiscoverPublishOrderStatusRequest;
import com.nexus.platform.dto.OpsDiscoverPublishPreviewResponse;
import com.nexus.platform.dto.OpsDiscoverSlotUpdateRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.OpsDiscoverAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ops/discover")
@RequiredArgsConstructor
public class OpsDiscoverAdminController {
    private final OpsDiscoverAdminService opsDiscoverAdminService;

    @GetMapping("/config")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<OpsDiscoverConfigResponse> getConfig() {
        return opsDiscoverAdminService.getConfig();
    }

    @GetMapping("/analytics")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<OpsDiscoverAnalyticsDto> getAnalytics() {
        return opsDiscoverAdminService.getAnalytics();
    }

    @PutMapping("/config")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsDiscoverConfigResponse> updateConfig(
            @RequestBody OpsDiscoverConfigUpdateRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        return opsDiscoverAdminService.updateConfig(request, currentUser, httpRequest.getRequestURI());
    }

    @GetMapping("/publish-preview/{scopeCode}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<OpsDiscoverPublishPreviewResponse> previewPublishScope(@PathVariable String scopeCode) {
        return opsDiscoverAdminService.previewPublishScope(scopeCode);
    }

    @PostMapping("/publish-orders")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsDiscoverConfigResponse> createPublishOrder(
            @RequestBody OpsDiscoverPublishOrderCreateRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsDiscoverAdminService.createPublishOrder(request, currentUser, httpRequest.getRequestURI());
    }

    @PutMapping("/publish-orders/{id}/status")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsDiscoverConfigResponse> updatePublishOrderStatus(
            @PathVariable Long id,
            @RequestBody OpsDiscoverPublishOrderStatusRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsDiscoverAdminService.updatePublishOrderStatus(id, request, currentUser, httpRequest.getRequestURI());
    }

    @PostMapping("/experiments")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsDiscoverConfigResponse> saveExperiment(
            @RequestBody OpsDiscoverExperimentRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsDiscoverAdminService.saveExperiment(request, currentUser, httpRequest.getRequestURI());
    }

    @PutMapping("/experiments/{id}/status")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsDiscoverConfigResponse> updateExperimentStatus(
            @PathVariable Long id,
            @RequestBody OpsDiscoverExperimentStatusRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsDiscoverAdminService.updateExperimentStatus(id, request, currentUser, httpRequest.getRequestURI());
    }

    @PostMapping("/content-items/batch-status")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsDiscoverConfigResponse> batchUpdateContentItemStatus(
            @RequestBody OpsDiscoverBatchStatusRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsDiscoverAdminService.batchUpdateContentItemStatus(
                request,
                currentUser,
                httpRequest.getRequestURI()
        );
    }

    @PostMapping("/batch-operation")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsDiscoverConfigResponse> batchOperate(
            @RequestBody OpsDiscoverBatchOperationRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsDiscoverAdminService.batchOperate(
                request,
                currentUser,
                httpRequest.getRequestURI()
        );
    }

    @PutMapping("/slots/{slotCode}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsDiscoverConfigResponse> updateSlotControl(
            @PathVariable String slotCode,
            @RequestBody OpsDiscoverSlotUpdateRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsDiscoverAdminService.updateSlotControl(slotCode, request, currentUser, httpRequest.getRequestURI());
    }

    @GetMapping("/categories")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<OpsDiscoverCategoryResponse>> listCategories() {
        return opsDiscoverAdminService.listCategories();
    }

    @PostMapping("/categories")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<List<OpsDiscoverCategoryResponse>> createCategory(@RequestBody OpsDiscoverCategoryRequest request) {
        return opsDiscoverAdminService.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<List<OpsDiscoverCategoryResponse>> updateCategory(
            @PathVariable Long id,
            @RequestBody OpsDiscoverCategoryRequest request
    ) {
        return opsDiscoverAdminService.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<List<OpsDiscoverCategoryResponse>> deleteCategory(@PathVariable Long id) {
        return opsDiscoverAdminService.deleteCategory(id);
    }
}
