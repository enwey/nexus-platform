package com.nexus.platform.controller;

import com.nexus.platform.config.AuthInterceptor;
import com.nexus.platform.dto.GameOpsDtos.GameOpsProfileResponse;
import com.nexus.platform.dto.GameOpsDtos.GameOpsProfileUpdateRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.GameOpsProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ops/game-profile")
@RequiredArgsConstructor
public class OpsGameProfileAdminController {
    private final GameOpsProfileService gameOpsProfileService;

    @GetMapping("/{gameId}")
    public Result<GameOpsProfileResponse> getProfile(@PathVariable Long gameId) {
        return gameOpsProfileService.getProfile(gameId);
    }

    @PutMapping("/{gameId}")
    public Result<GameOpsProfileResponse> updateProfile(
            @PathVariable Long gameId,
            @RequestBody GameOpsProfileUpdateRequest request,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User currentUser) {
        return gameOpsProfileService.updateProfile(gameId, request, currentUser);
    }
}
