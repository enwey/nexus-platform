package com.nexus.platform.security;

import com.nexus.platform.entity.OpsRolePermissionOverride;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.OpsRolePermissionOverrideRepository;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class RolePermissionService {

    private static final Map<User.UserRole, EnumSet<Permission>> DEFAULT_ROLE_PERMISSIONS = Map.of(
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
                    Permission.GAME_DEVELOPER_WRITE,
                    Permission.GAME_AUDIT_SUBMIT,
                    Permission.GAME_VERSION_ROLLBACK
            ),
            User.UserRole.PLAYER, EnumSet.of(
                    Permission.USER_PROFILE_READ,
                    Permission.USER_PROFILE_WRITE,
                    Permission.USER_WALLET_READ,
                    Permission.USER_LOGOUT,
                    Permission.LIBRARY_READ,
                    Permission.LIBRARY_WRITE
            )
    );

    private static final Set<Permission> ADMIN_SAFETY_BASELINE = Set.of(
            Permission.USER_PROFILE_READ,
            Permission.USER_PROFILE_WRITE,
            Permission.AUDIT_LOG_READ,
            Permission.ANDROID_ADMIN_READ,
            Permission.ANDROID_ADMIN_WRITE
    );

    private final OpsRolePermissionOverrideRepository overrideRepository;
    private final AtomicReference<Map<User.UserRole, EnumSet<Permission>>> effectivePermissionsRef =
            new AtomicReference<>(Collections.unmodifiableMap(DEFAULT_ROLE_PERMISSIONS));

    public RolePermissionService(OpsRolePermissionOverrideRepository overrideRepository) {
        this.overrideRepository = overrideRepository;
        refreshOverrides();
    }

    public boolean hasPermission(User user, Permission permission) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        EnumSet<Permission> permissions = effectivePermissionsRef.get().get(user.getRole());
        return permissions != null && permissions.contains(permission);
    }

    public boolean hasPermission(Authentication authentication, Permission permission) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return false;
        }
        return hasPermission(user, permission);
    }

    public Map<User.UserRole, EnumSet<Permission>> getRolePermissionsSnapshot() {
        return Collections.unmodifiableMap(effectivePermissionsRef.get());
    }

    public List<Permission> getAllPermissions() {
        return List.of(Permission.values());
    }

    public boolean isRoleEditable(User.UserRole role) {
        return role != null;
    }

    public void replaceRolePermissions(User.UserRole role, Set<Permission> permissions, String updatedBy) {
        EnumSet<Permission> nextPermissions = permissions == null || permissions.isEmpty()
                ? EnumSet.noneOf(Permission.class)
                : EnumSet.copyOf(permissions);
        if (role == User.UserRole.ADMIN) {
            nextPermissions.addAll(ADMIN_SAFETY_BASELINE);
        }
        overrideRepository.deleteByRoleCode(role.name());
        for (Permission permission : nextPermissions) {
            OpsRolePermissionOverride override = new OpsRolePermissionOverride();
            override.setRoleCode(role.name());
            override.setPermissionCode(permission.name());
            override.setEnabled(true);
            override.setUpdatedBy(updatedBy);
            overrideRepository.save(override);
        }
        refreshOverrides();
    }

    public void refreshOverrides() {
        Map<User.UserRole, EnumSet<Permission>> merged = new HashMap<>();
        DEFAULT_ROLE_PERMISSIONS.forEach((role, permissions) -> merged.put(role, EnumSet.copyOf(permissions)));
        Map<User.UserRole, List<OpsRolePermissionOverride>> grouped = overrideRepository.findAllByOrderByRoleCodeAscPermissionCodeAsc()
                .stream()
                .filter(OpsRolePermissionOverride::isEnabled)
                .collect(Collectors.groupingBy(item -> User.UserRole.valueOf(item.getRoleCode())));
        grouped.forEach((role, overrides) -> {
            EnumSet<Permission> set = EnumSet.noneOf(Permission.class);
            for (OpsRolePermissionOverride override : overrides) {
                set.add(Permission.valueOf(override.getPermissionCode()));
            }
            if (role == User.UserRole.ADMIN) {
                set.addAll(ADMIN_SAFETY_BASELINE);
            }
            merged.put(role, set);
        });
        effectivePermissionsRef.set(Collections.unmodifiableMap(merged));
    }
}
