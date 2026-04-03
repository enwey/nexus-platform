package com.nexus.platform.config;

import com.nexus.platform.entity.User;
import com.nexus.platform.security.Permission;
import com.nexus.platform.security.RolePermissionService;
import com.nexus.platform.service.AuthTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    public static final String AUTH_USER_ATTRIBUTE = "AUTH_USER";

    private final AuthTokenService authTokenService;
    private final RolePermissionService rolePermissionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Permission requiredPermission = resolveRequiredPermission(request);
        if (requiredPermission == null) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing valid authorization token");
            return false;
        }

        String token = authorization.substring("Bearer ".length()).trim();
        User user = authTokenService.resolveUser(token);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is invalid or expired");
            return false;
        }

        if (!rolePermissionService.hasPermission(user, requiredPermission)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "No permission");
            return false;
        }

        request.setAttribute(AUTH_USER_ATTRIBUTE, user);
        return true;
    }

    private Permission resolveRequiredPermission(HttpServletRequest request) {
        String uri = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (uri == null || uri.isBlank()) {
            uri = request.getRequestURI();
        }
        String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method) && "/user/send-code".equals(uri)) {
            return null;
        }
        if ("POST".equalsIgnoreCase(method) && "/user/password/reset".equals(uri)) {
            return null;
        }
        if ("/user/me".equals(uri)) {
            return Permission.USER_PROFILE_READ;
        }
        if ("GET".equalsIgnoreCase(method) && "/user/profile".equals(uri)) {
            return Permission.USER_PROFILE_READ;
        }
        if ("POST".equalsIgnoreCase(method) && "/user/profile".equals(uri)) {
            return Permission.USER_PROFILE_WRITE;
        }
        if ("POST".equalsIgnoreCase(method) && "/user/password/change".equals(uri)) {
            return Permission.USER_PROFILE_WRITE;
        }
        if ("GET".equalsIgnoreCase(method) && "/user/devices".equals(uri)) {
            return Permission.USER_PROFILE_READ;
        }
        if ("POST".equalsIgnoreCase(method) && uri.startsWith("/user/devices/")) {
            return Permission.USER_PROFILE_WRITE;
        }
        if ("POST".equalsIgnoreCase(method) && "/user/logout".equals(uri)) {
            return Permission.USER_LOGOUT;
        }
        if ("POST".equalsIgnoreCase(method) && "/user/logout-all".equals(uri)) {
            return Permission.USER_LOGOUT;
        }
        if ("POST".equalsIgnoreCase(method) && "/user/terminate".equals(uri)) {
            return Permission.USER_PROFILE_WRITE;
        }
        if ("GET".equalsIgnoreCase(method) && uri.startsWith("/wallet/")) {
            return Permission.USER_WALLET_READ;
        }
        if ("GET".equalsIgnoreCase(method) && uri.startsWith("/referral/")) {
            return Permission.USER_WALLET_READ;
        }
        if ("POST".equalsIgnoreCase(method) && uri.startsWith("/referral/")) {
            return Permission.USER_WALLET_READ;
        }
        if ("GET".equalsIgnoreCase(method) && uri.startsWith("/library/")) {
            return Permission.LIBRARY_READ;
        }
        if (("POST".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method))
                && uri.startsWith("/library/")) {
            return Permission.LIBRARY_WRITE;
        }
        if ("POST".equalsIgnoreCase(method) && "/game/upload".equals(uri)) {
            return Permission.GAME_UPLOAD;
        }
        if ("POST".equalsIgnoreCase(method) && uri.startsWith("/game/submit/")) {
            return Permission.GAME_AUDIT_SUBMIT;
        }
        if ("POST".equalsIgnoreCase(method) && uri.matches("^/game/\\d+/submit-version/\\d+$")) {
            return Permission.GAME_AUDIT_SUBMIT;
        }
        if ("POST".equalsIgnoreCase(method) && uri.matches("^/game/\\d+/rollback/\\d+$")) {
            return Permission.GAME_VERSION_ROLLBACK;
        }
        if ("GET".equalsIgnoreCase(method) && uri.matches("^/game/\\d+/versions$")) {
            return Permission.GAME_DEVELOPER_READ;
        }
        if (uri.startsWith("/game/developer/")) {
            return Permission.GAME_DEVELOPER_READ;
        }
        if ("PUT".equalsIgnoreCase(method) && uri.startsWith("/game/") && uri.endsWith("/metadata")) {
            return Permission.GAME_DEVELOPER_WRITE;
        }
        if ("POST".equalsIgnoreCase(method) && uri.startsWith("/game/approve/")) {
            return Permission.GAME_AUDIT_APPROVE;
        }
        if ("POST".equalsIgnoreCase(method) && uri.startsWith("/game/reject/")) {
            return Permission.GAME_AUDIT_REJECT;
        }
        if ("GET".equalsIgnoreCase(method) && uri.startsWith("/audit/logs")) {
            return Permission.AUDIT_LOG_READ;
        }
        if (uri.startsWith("/admin/android/config") && "PUT".equalsIgnoreCase(method)) {
            return Permission.ANDROID_ADMIN_WRITE;
        }
        if (uri.startsWith("/admin/ops/discover/config") && "PUT".equalsIgnoreCase(method)) {
            return Permission.ANDROID_ADMIN_WRITE;
        }
        if (uri.startsWith("/admin/ops/")) {
            return Permission.ANDROID_ADMIN_READ;
        }
        if (uri.startsWith("/admin/android/")) {
            return Permission.ANDROID_ADMIN_READ;
        }
        return null;
    }
}
