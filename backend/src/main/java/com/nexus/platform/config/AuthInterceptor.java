package com.nexus.platform.config;

import com.nexus.platform.entity.User;
import com.nexus.platform.service.AuthTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    public static final String AUTH_USER_ATTRIBUTE = "AUTH_USER";

    private final AuthTokenService authTokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorization = request.getHeader("Authorization");
        String requestUri = request.getRequestURI();
        boolean requiresAuth = requiresAuthentication(requestUri);

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            if (requiresAuth) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "缺少有效的登录凭证");
                return false;
            }
            return true;
        }

        String token = authorization.substring("Bearer ".length()).trim();
        User user = authTokenService.resolveUser(token);
        if (user == null) {
            if (requiresAuth) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "登录凭证已失效");
                return false;
            }
            return true;
        }

        if ((requestUri.contains("/game/approve/") || requestUri.contains("/game/reject/"))
                && user.getRole() != User.UserRole.ADMIN) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "当前账号没有执行审核操作的权限");
            return false;
        }

        request.setAttribute(AUTH_USER_ATTRIBUTE, user);
        return true;
    }

    private boolean requiresAuthentication(String requestUri) {
        return requestUri.contains("/user/me")
                || requestUri.contains("/game/upload")
                || requestUri.contains("/game/developer/")
                || requestUri.contains("/game/approve/")
                || requestUri.contains("/game/reject/");
    }
}
