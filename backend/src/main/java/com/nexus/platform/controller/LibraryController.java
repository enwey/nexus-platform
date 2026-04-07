package com.nexus.platform.controller;

import com.nexus.platform.dto.LibraryHomeResponse;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/library")
@RequiredArgsConstructor
public class LibraryController {
    private final LibraryService libraryService;

    @GetMapping("/home")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).LIBRARY_READ)")
    public Result<LibraryHomeResponse> home(
            @AuthenticationPrincipal User currentUser) {
        return libraryService.getHome(currentUser);
    }

    @PostMapping("/{appId}/play")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).LIBRARY_WRITE)")
    public Result<Void> markPlayed(
            @PathVariable String appId,
            @AuthenticationPrincipal User currentUser) {
        return libraryService.markPlayed(currentUser, appId);
    }

    @PostMapping("/{appId}/favorite")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).LIBRARY_WRITE)")
    public Result<Void> addFavorite(
            @PathVariable String appId,
            @AuthenticationPrincipal User currentUser) {
        return libraryService.setFavorite(currentUser, appId, true);
    }

    @DeleteMapping("/{appId}/favorite")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).LIBRARY_WRITE)")
    public Result<Void> removeFavorite(
            @PathVariable String appId,
            @AuthenticationPrincipal User currentUser) {
        return libraryService.setFavorite(currentUser, appId, false);
    }

    @PostMapping("/{appId}/share")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).LIBRARY_WRITE)")
    public Result<Void> markShared(
            @PathVariable String appId,
            @AuthenticationPrincipal User currentUser) {
        return libraryService.markShared(currentUser, appId);
    }
}
