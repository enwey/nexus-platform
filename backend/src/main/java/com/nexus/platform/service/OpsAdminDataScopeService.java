package com.nexus.platform.service;

import com.nexus.platform.dto.OpsAdminDataScopeDto;
import com.nexus.platform.dto.OpsAdminDataScopeUpdateRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.OpsAdminDataScope;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.OpsAdminDataScopeRepository;
import com.nexus.platform.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OpsAdminDataScopeService {
    public static final String ALL_DEVELOPERS = "ALL_DEVELOPERS";
    public static final String HIGH_RISK_ONLY = "HIGH_RISK_ONLY";
    public static final String PENDING_CERT_ONLY = "PENDING_CERT_ONLY";
    public static final String BLOCKED_ONLY = "BLOCKED_ONLY";

    private final OpsAdminDataScopeRepository dataScopeRepository;
    private final UserRepository userRepository;

    public List<OpsAdminDataScopeDto> listAdminScopes() {
        return userRepository.findByRoleOrderByCreatedAtDesc(User.UserRole.ADMIN).stream()
                .map(user -> new OpsAdminDataScopeDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        resolveScopeForUser(user)
                ))
                .toList();
    }

    public String resolveScopeForUser(User user) {
        if (user == null || user.getRole() != User.UserRole.ADMIN) {
            return BLOCKED_ONLY;
        }
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            return ALL_DEVELOPERS;
        }
        return dataScopeRepository.findByAdminUserId(user.getId())
                .map(OpsAdminDataScope::getScopeCode)
                .filter(this::isValidScopeCode)
                .orElse(HIGH_RISK_ONLY);
    }

    @Transactional
    public Result<OpsAdminDataScopeDto> updateAdminScope(Long adminUserId, OpsAdminDataScopeUpdateRequest request) {
        if (adminUserId == null || adminUserId <= 0) {
            return Result.error("Invalid admin user id");
        }
        User admin = userRepository.findById(adminUserId).orElse(null);
        if (admin == null || admin.getRole() != User.UserRole.ADMIN) {
            return Result.error("Admin user not found");
        }
        String scopeCode = normalizeScopeCode(request == null ? null : request.scopeCode());
        if (scopeCode == null) {
            return Result.error("Invalid data scope code");
        }
        OpsAdminDataScope scope = dataScopeRepository.findByAdminUserId(adminUserId).orElseGet(() -> {
            OpsAdminDataScope created = new OpsAdminDataScope();
            created.setAdminUserId(adminUserId);
            return created;
        });
        scope.setScopeCode(scopeCode);
        dataScopeRepository.save(scope);
        return Result.success(new OpsAdminDataScopeDto(admin.getId(), admin.getUsername(), admin.getEmail(), scopeCode));
    }

    private boolean isValidScopeCode(String value) {
        return normalizeScopeCode(value) != null;
    }

    private String normalizeScopeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case ALL_DEVELOPERS, HIGH_RISK_ONLY, PENDING_CERT_ONLY, BLOCKED_ONLY -> normalized;
            default -> null;
        };
    }
}
