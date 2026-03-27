package com.nexus.platform.config;

import com.nexus.platform.entity.User;
import com.nexus.platform.security.Permission;
import com.nexus.platform.security.RolePermissionService;
import com.nexus.platform.service.AuthTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.HandlerInterceptor;

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
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "缺少有效的登录凭证");
            return false;
        }

        String token = authorization.substring("Bearer ".length()).trim();
        User user = authTokenService.resolveUser(token);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "登录凭证已失效，请重新登录");
            return false;
        }

        if (!rolePermissionService.hasPermission(user, requiredPermission)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "当前账号没有执行该操作的权限");
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

        if ("/user/me".equals(uri)) {
            return Permission.USER_PROFILE_READ;
        }
        if ("POST".equalsIgnoreCase(method) && "/user/logout".equals(uri)) {
            return Permission.USER_LOGOUT;
        }
        if ("POST".equalsIgnoreCase(method) && "/game/upload".equals(uri)) {
            return Permission.GAME_UPLOAD;
        }
        if (uri.startsWith("/game/developer/")) {
            return Permission.GAME_DEVELOPER_READ;
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
        return null;
    }
}
