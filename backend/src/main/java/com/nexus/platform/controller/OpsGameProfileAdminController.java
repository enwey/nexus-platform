package com.nexus.platform.controller;

import com.nexus.platform.dto.GameOpsDtos.GameOpsProfileResponse;
import com.nexus.platform.dto.GameOpsDtos.GameOpsProfileUpdateRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.GameOpsProfileService;
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
@RequestMapping("/admin/ops/game-profile")
@RequiredArgsConstructor
public class OpsGameProfileAdminController {
    private final GameOpsProfileService gameOpsProfileService;

    @GetMapping("/{gameId}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<GameOpsProfileResponse> getProfile(@PathVariable Long gameId) {
        return gameOpsProfileService.getProfile(gameId);
    }

    @PutMapping("/{gameId}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<GameOpsProfileResponse> updateProfile(
            @PathVariable Long gameId,
            @RequestBody GameOpsProfileUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return gameOpsProfileService.updateProfile(gameId, request, currentUser);
    }
}
