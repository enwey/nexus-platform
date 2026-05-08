package com.nexus.platform.controller;

import com.nexus.platform.dto.GameMetadataUpdateRequest;
import com.nexus.platform.dto.OpsBatchActionResultDto;
import com.nexus.platform.dto.OpsAdminReviewerOptionDto;
import com.nexus.platform.dto.OpsGameGovernanceItemDto;
import com.nexus.platform.dto.OpsGameGovernanceImpactDto;
import com.nexus.platform.dto.OpsGameGovernanceScopeDto;
import com.nexus.platform.dto.OpsGameGovernanceScopeUpdateRequest;
import com.nexus.platform.dto.OpsGameVersionDirectPublishRequest;
import com.nexus.platform.dto.OpsGameVisibilityBatchUpdateRequest;
import com.nexus.platform.dto.OpsGameVisibilityUpdateRequest;
import com.nexus.platform.dto.OpsReviewOverviewDto;
import com.nexus.platform.dto.OpsReviewAssignmentRequest;
import com.nexus.platform.dto.OpsReviewBatchActionRequest;
import com.nexus.platform.dto.OpsReviewItemDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.OpsGameGovernanceService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/ops")
@RequiredArgsConstructor
public class OpsGameGovernanceAdminController {
    private final OpsGameGovernanceService opsGameGovernanceService;

    @GetMapping("/games")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<OpsGameGovernanceItemDto>> listGames(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long developerId,
            @RequestParam(required = false) String keyword
    ) {
        return opsGameGovernanceService.listGames(status, category, developerId, keyword);
    }

    @PostMapping("/games/upload")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<Game> uploadGame(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "requiresOnline", required = false) Boolean requiresOnline,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsGameGovernanceService.uploadGameAsAdmin(
                file,
                name,
                description,
                category,
                tags,
                requiresOnline,
                currentUser,
                httpRequest.getRequestURI()
        );
    }

    @PostMapping("/games/{gameId}/versions/upload")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<com.nexus.platform.entity.GameVersion> uploadGameVersion(
            @PathVariable Long gameId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "versionName", required = false) String versionName,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsGameGovernanceService.uploadGameVersionAsAdmin(
                gameId,
                file,
                versionName,
                currentUser,
                httpRequest.getRequestURI()
        );
    }

    @GetMapping("/reviews")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<OpsReviewItemDto>> listReviews(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long developerId
    ) {
        return opsGameGovernanceService.listReviews(status, keyword, developerId);
    }

    @GetMapping("/reviews/overview")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<OpsReviewOverviewDto> getReviewOverview() {
        return opsGameGovernanceService.getReviewOverview();
    }

    @GetMapping("/reviewers")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<OpsAdminReviewerOptionDto>> listReviewers() {
        return opsGameGovernanceService.listReviewers();
    }

    @PostMapping("/reviews/{versionId}/assign")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsReviewItemDto> assignReview(
            @PathVariable Long versionId,
            @RequestBody OpsReviewAssignmentRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsGameGovernanceService.assignReviewTask(versionId, request, currentUser, httpRequest.getRequestURI());
    }

    @PostMapping("/reviews/batch")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsBatchActionResultDto> batchReviewAction(
            @RequestBody OpsReviewBatchActionRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsGameGovernanceService.batchReviewAction(
                request,
                currentUser,
                httpRequest.getRequestURI()
        );
    }

    @PutMapping("/games/{gameId}/metadata")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<Game> updateGameMetadata(
            @PathVariable Long gameId,
            @RequestBody GameMetadataUpdateRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsGameGovernanceService.updateGameMetadataAsAdmin(
                gameId,
                request,
                currentUser,
                httpRequest.getRequestURI()
        );
    }

    @PostMapping("/games/{gameId}/versions/{versionId}/direct-publish")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<Void> directPublishVersion(
            @PathVariable Long gameId,
            @PathVariable Long versionId,
            @RequestBody(required = false) OpsGameVersionDirectPublishRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsGameGovernanceService.directPublishVersionAsAdmin(
                gameId,
                versionId,
                request,
                currentUser,
                httpRequest.getRequestURI()
        );
    }

    @PutMapping("/games/{gameId}/visibility")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<Game> updateGameVisibility(
            @PathVariable Long gameId,
            @RequestBody OpsGameVisibilityUpdateRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsGameGovernanceService.updateGameVisibilityAsAdmin(
                gameId,
                request,
                currentUser,
                httpRequest.getRequestURI()
        );
    }

    @PutMapping("/games/{gameId}/governance-scope")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsGameGovernanceScopeDto> updateGameGovernanceScope(
            @PathVariable Long gameId,
            @RequestBody OpsGameGovernanceScopeUpdateRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsGameGovernanceService.updateGameGovernanceScopeAsAdmin(
                gameId,
                request,
                currentUser,
                httpRequest.getRequestURI()
        );
    }

    @GetMapping("/games/{gameId}/governance-impact")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<OpsGameGovernanceImpactDto> getGovernanceImpact(@PathVariable Long gameId) {
        return opsGameGovernanceService.getGovernanceImpact(gameId);
    }

    @PutMapping("/games/visibility/batch")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsBatchActionResultDto> updateGameVisibilityBatch(
            @RequestBody OpsGameVisibilityBatchUpdateRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsGameGovernanceService.updateGameVisibilityBatchAsAdmin(
                request,
                currentUser,
                httpRequest.getRequestURI()
        );
    }
}
