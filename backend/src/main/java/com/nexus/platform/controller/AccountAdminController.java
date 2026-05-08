package com.nexus.platform.controller;

import com.nexus.platform.dto.DeveloperCertificationProfileDto;
import com.nexus.platform.dto.DeveloperCertificationReviewRecordDto;
import com.nexus.platform.dto.DeveloperGovernanceRecordDto;
import com.nexus.platform.dto.OpsDeveloperCertificationReviewRequest;
import com.nexus.platform.dto.OpsDeveloperAccountDto;
import com.nexus.platform.dto.OpsDeveloperGovernanceUpdateRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.AccountOpsService;
import com.nexus.platform.service.DeveloperCertificationService;
import java.util.List;
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
@RequestMapping("/admin/ops/accounts")
@RequiredArgsConstructor
public class AccountAdminController {
    private final AccountOpsService accountOpsService;
    private final DeveloperCertificationService developerCertificationService;

    @GetMapping("/developers")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).AUDIT_LOG_READ)")
    public Result<List<OpsDeveloperAccountDto>> getDeveloperAccounts(@AuthenticationPrincipal User currentUser) {
        return accountOpsService.listDeveloperAccounts(currentUser);
    }

    @GetMapping("/developers/{developerId}/governance-records")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).AUDIT_LOG_READ)")
    public Result<List<DeveloperGovernanceRecordDto>> getDeveloperGovernanceRecords(@PathVariable Long developerId) {
        return accountOpsService.listDeveloperGovernanceRecords(developerId);
    }

    @GetMapping("/developers/{developerId}/certification-profile")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).AUDIT_LOG_READ)")
    public Result<DeveloperCertificationProfileDto> getDeveloperCertificationProfile(@PathVariable Long developerId) {
        return developerCertificationService.getDeveloperProfile(developerId);
    }

    @GetMapping("/developers/{developerId}/certification-reviews")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).AUDIT_LOG_READ)")
    public Result<List<DeveloperCertificationReviewRecordDto>> getDeveloperCertificationReviews(@PathVariable Long developerId) {
        return developerCertificationService.getDeveloperReviewRecords(developerId);
    }

    @PutMapping("/developers/{developerId}/governance")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsDeveloperAccountDto> updateDeveloperGovernance(
            @PathVariable Long developerId,
            @RequestBody OpsDeveloperGovernanceUpdateRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return accountOpsService.updateDeveloperGovernance(developerId, request, currentUser, httpRequest.getRequestURI());
    }

    @PutMapping("/developers/{developerId}/certification-review")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<DeveloperCertificationProfileDto> reviewDeveloperCertification(
            @PathVariable Long developerId,
            @RequestBody OpsDeveloperCertificationReviewRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return developerCertificationService.reviewDeveloperProfile(developerId, request, currentUser, httpRequest.getRequestURI());
    }
}
