package com.nexus.platform.controller;

import com.nexus.platform.dto.AuthResponse;
import com.nexus.platform.dto.DeviceSessionDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.dto.UserProfileDetailDto;
import com.nexus.platform.dto.UserProfileDto;
import com.nexus.platform.dto.VerificationCodeResponse;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.AccountOpsService;
import com.nexus.platform.service.AccountService;
import com.nexus.platform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AccountService accountService;
    private final AccountOpsService accountOpsService;

    @PostMapping("/register")
    public Result<AuthResponse> register(@RequestBody RegisterRequest request) {
        return userService.register(request.username(), request.password(), request.email());
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return userService.login(request.username(), request.password(), extractClientIp(httpRequest));
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        return userService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_LOGOUT)")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        if (token != null) {
            userService.logout(token);
        }
        return Result.success();
    }

    @PostMapping("/send-code")
    public Result<VerificationCodeResponse> sendCode(@RequestBody SendCodeRequest request) {
        return accountOpsService.sendCode(request.account(), request.purpose());
    }

    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        return accountOpsService.resetPassword(request.account(), request.code(), request.newPassword());
    }

    @PostMapping("/password/change")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<Void> changePassword(
            @AuthenticationPrincipal User user,
            @RequestBody ChangePasswordRequest request) {
        return accountOpsService.changePassword(user, request.oldPassword(), request.newPassword());
    }

    @GetMapping("/devices")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_READ)")
    public Result<List<DeviceSessionDto>> devices(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        String currentDeviceId = token == null ? null : accountService.resolveDeviceIdFromToken(token);
        return accountOpsService.listDevices(user.getId(), currentDeviceId);
    }

    @PostMapping("/devices/{deviceId}/kick")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<Void> kickDevice(
            @AuthenticationPrincipal User user,
            @PathVariable String deviceId) {
        return accountOpsService.kickDevice(user.getId(), deviceId);
    }

    @PostMapping("/logout-all")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_LOGOUT)")
    public Result<Void> logoutAll(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        String currentDeviceId = token == null ? null : accountService.resolveDeviceIdFromToken(token);
        return accountOpsService.logoutAll(user.getId(), currentDeviceId);
    }

    @PostMapping("/terminate")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<Void> terminate(
            @AuthenticationPrincipal User user,
            @RequestBody TerminateRequest request) {
        return accountOpsService.terminateAccount(user, request.confirmText());
    }

    @GetMapping("/me")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_READ)")
    public Result<UserProfileDto> getCurrentUser(@AuthenticationPrincipal User user) {
        return Result.success(UserProfileDto.from(user));
    }

    @GetMapping("/profile")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_READ)")
    public Result<UserProfileDetailDto> getProfile(
            @AuthenticationPrincipal User user) {
        return accountService.getProfile(user);
    }

    @PostMapping("/profile")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<UserProfileDetailDto> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateProfileRequest request) {
        return accountService.updateProfile(
                user,
                request.displayName(),
                request.avatarUrl(),
                request.languageTag(),
                request.email(),
                request.phone()
        );
    }

    @GetMapping("/{id}")
    public Result<UserProfileDto> getUser(@PathVariable Long id) {
        UserProfileDto user = userService.findById(id);
        if (user == null) {
            return Result.error("User not found");
        }
        return Result.success(user);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            if (parts.length > 0) {
                return parts[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}

record RegisterRequest(String username, String password, String email) {}
record LoginRequest(String username, String password) {}
record RefreshRequest(String refreshToken) {}
record SendCodeRequest(String account, String purpose) {}
record ResetPasswordRequest(String account, String code, String newPassword) {}
record ChangePasswordRequest(String oldPassword, String newPassword) {}
record TerminateRequest(String confirmText) {}
record UpdateProfileRequest(
        String displayName,
        String avatarUrl,
        String languageTag,
        String email,
        String phone
) {}
