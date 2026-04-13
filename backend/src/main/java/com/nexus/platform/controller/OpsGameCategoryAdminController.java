package com.nexus.platform.controller;

import com.nexus.platform.dto.OpsGameCategoryRequest;
import com.nexus.platform.dto.OpsGameCategoryResponse;
import com.nexus.platform.dto.Result;
import com.nexus.platform.service.GameService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ops/game-categories")
@RequiredArgsConstructor
public class OpsGameCategoryAdminController {
    private final GameService gameService;

    @GetMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<OpsGameCategoryResponse>> listCategories() {
        return gameService.listGameCategoryOptions();
    }

    @PostMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<List<OpsGameCategoryResponse>> createCategory(@RequestBody OpsGameCategoryRequest request) {
        return gameService.createGameCategory(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<List<OpsGameCategoryResponse>> updateCategory(
            @PathVariable Long id,
            @RequestBody OpsGameCategoryRequest request
    ) {
        return gameService.updateGameCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<List<OpsGameCategoryResponse>> deleteCategory(@PathVariable Long id) {
        return gameService.deleteGameCategory(id);
    }
}
