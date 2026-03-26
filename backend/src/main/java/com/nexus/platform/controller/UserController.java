package com.nexus.platform.controller;

import com.nexus.platform.config.AuthInterceptor;
import com.nexus.platform.dto.AuthResponse;
import com.nexus.platform.dto.Result;
import com.nexus.platform.dto.UserProfileDto;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

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
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            userService.logout(authorization.substring("Bearer ".length()).trim());
        }
        return Result.success();
    }

    @GetMapping("/me")
    public Result<UserProfileDto> getCurrentUser(@RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User user) {
        return Result.success(UserProfileDto.from(user));
    }

    @GetMapping("/{id}")
    public Result<UserProfileDto> getUser(@PathVariable Long id) {
        UserProfileDto user = userService.findById(id);
        if (user == null) {
            return Result.error("用户不存在");
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
}

record RegisterRequest(String username, String password, String email) {}
record LoginRequest(String username, String password) {}
record RefreshRequest(String refreshToken) {}

