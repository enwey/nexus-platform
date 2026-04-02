package com.nexus.platform.controller;

import com.nexus.platform.config.AuthInterceptor;
import com.nexus.platform.dto.LibraryHomeResponse;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/library")
@RequiredArgsConstructor
public class LibraryController {
    private final LibraryService libraryService;

    @GetMapping("/home")
    public Result<LibraryHomeResponse> home(
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User currentUser) {
        return libraryService.getHome(currentUser);
    }

    @PostMapping("/{appId}/play")
    public Result<Void> markPlayed(
            @PathVariable String appId,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User currentUser) {
        return libraryService.markPlayed(currentUser, appId);
    }

    @PostMapping("/{appId}/favorite")
    public Result<Void> addFavorite(
            @PathVariable String appId,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User currentUser) {
        return libraryService.setFavorite(currentUser, appId, true);
    }

    @DeleteMapping("/{appId}/favorite")
    public Result<Void> removeFavorite(
            @PathVariable String appId,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User currentUser) {
        return libraryService.setFavorite(currentUser, appId, false);
    }

    @PostMapping("/{appId}/share")
    public Result<Void> markShared(
            @PathVariable String appId,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User currentUser) {
        return libraryService.markShared(currentUser, appId);
    }
}
