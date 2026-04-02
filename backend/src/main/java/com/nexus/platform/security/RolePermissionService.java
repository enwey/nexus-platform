package com.nexus.platform.security;

import com.nexus.platform.entity.User;
import java.util.EnumSet;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RolePermissionService {

    private static final Map<User.UserRole, EnumSet<Permission>> ROLE_PERMISSIONS = Map.of(
            User.UserRole.ADMIN, EnumSet.allOf(Permission.class),
            User.UserRole.DEVELOPER, EnumSet.of(
                    Permission.USER_PROFILE_READ,
                    Permission.USER_PROFILE_WRITE,
                    Permission.USER_WALLET_READ,
                    Permission.USER_LOGOUT,
                    Permission.LIBRARY_READ,
                    Permission.LIBRARY_WRITE,
                    Permission.GAME_UPLOAD,
                    Permission.GAME_DEVELOPER_READ,
                    Permission.GAME_AUDIT_SUBMIT,
                    Permission.GAME_VERSION_ROLLBACK
            )
    );

    public boolean hasPermission(User user, Permission permission) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        EnumSet<Permission> permissions = ROLE_PERMISSIONS.get(user.getRole());
        return permissions != null && permissions.contains(permission);
    }
}
