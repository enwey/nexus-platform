package com.nexus.platform.controller;

import com.nexus.platform.dto.GameReviewAppealDecisionRequest;
import com.nexus.platform.dto.GameReviewAppealDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.GameReviewAppealService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ops/review-appeals")
@RequiredArgsConstructor
public class OpsReviewAppealAdminController {
    private final GameReviewAppealService gameReviewAppealService;

    @GetMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<GameReviewAppealDto>> listAppeals() {
        return gameReviewAppealService.listOpsAppeals();
    }

    @PostMapping("/{appealId}/decision")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<GameReviewAppealDto> reviewAppeal(
            @PathVariable Long appealId,
            @RequestBody GameReviewAppealDecisionRequest request,
            @AuthenticationPrincipal User currentUser,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return gameReviewAppealService.reviewAppeal(appealId, request, currentUser, httpRequest.getRequestURI());
    }
}
