package com.nexus.platform.service;

import com.nexus.platform.dto.DeveloperApiKeyCreateRequest;
import com.nexus.platform.dto.DeveloperApiKeyCreateResponse;
import com.nexus.platform.dto.DeveloperApiKeyDto;
import com.nexus.platform.dto.DeveloperApiKeyRevokeRequest;
import com.nexus.platform.dto.DeveloperApiKeyRotateRequest;
import com.nexus.platform.dto.DeveloperTeamMemberDto;
import com.nexus.platform.dto.DeveloperTeamMemberStatusRequest;
import com.nexus.platform.dto.DeveloperTeamMemberUpsertRequest;
import com.nexus.platform.dto.DeveloperWorkspaceAuditLogDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.DeveloperApiKey;
import com.nexus.platform.entity.DeveloperTeamMember;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.DeveloperApiKeyRepository;
import com.nexus.platform.repository.DeveloperTeamMemberRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeveloperWorkspaceService {
    private static final Set<String> TEAM_ROLES = Set.of("OWNER", "ADMIN", "RELEASE_MANAGER", "ANALYST", "FINANCE");
    private static final Set<String> TEAM_STATUSES = Set.of("INVITED", "ACTIVE", "DISABLED");
    private static final Set<String> KEY_SCOPES = Set.of("PROFILE_READ", "RELEASE_READ", "RELEASE_WRITE", "ANALYTICS_READ");
    private static final Set<String> KEY_STATUSES = Set.of("ACTIVE", "REVOKED", "EXPIRED");
    private static final String REVOKE_CONFIRM_TEXT = "REVOKE";

    private final DeveloperTeamMemberRepository developerTeamMemberRepository;
    private final DeveloperApiKeyRepository developerApiKeyRepository;
    private final AuditLogService auditLogService;

    public Result<List<DeveloperTeamMemberDto>> listTeamMembers(User currentUser) {
        return Result.success(
                developerTeamMemberRepository.findByOwnerUserIdOrderByUpdatedAtDesc(currentUser.getId())
                        .stream()
                        .map(this::toTeamMemberDto)
                        .toList()
        );
    }

    @Transactional
    public Result<DeveloperTeamMemberDto> createTeamMember(
            User currentUser,
            DeveloperTeamMemberUpsertRequest request,
            String requestUri
    ) {
        ValidationResult validation = validateTeamMember(currentUser.getId(), request, null);
        if (!validation.success()) {
            auditLogService.logOpsAudit("DEV_TEAM_MEMBER_CREATE", currentUser, null, developerTarget(currentUser), false, validation.message(), requestUri);
            return Result.error(validation.message());
        }
        DeveloperTeamMember member = new DeveloperTeamMember();
        member.setOwnerUserId(currentUser.getId());
        member.setMemberName(validation.memberName());
        member.setMemberEmail(validation.memberEmail());
        member.setTeamRole(validation.teamRole());
        member.setMemberStatus(validation.memberStatus());
        member.setNote(validation.note());
        DeveloperTeamMember saved = developerTeamMemberRepository.save(member);
        auditLogService.logOpsAudit("DEV_TEAM_MEMBER_CREATE", currentUser, null, developerTarget(currentUser), true, saved.getMemberEmail(), requestUri);
        return Result.success(toTeamMemberDto(saved));
    }

    @Transactional
    public Result<DeveloperTeamMemberDto> updateTeamMember(
            User currentUser,
            Long memberId,
            DeveloperTeamMemberUpsertRequest request,
            String requestUri
    ) {
        DeveloperTeamMember member = developerTeamMemberRepository.findByIdAndOwnerUserId(memberId, currentUser.getId()).orElse(null);
        if (member == null) {
            auditLogService.logOpsAudit("DEV_TEAM_MEMBER_UPDATE", currentUser, null, developerTarget(currentUser), false, "Team member not found", requestUri);
            return Result.error("Team member not found");
        }
        ValidationResult validation = validateTeamMember(currentUser.getId(), request, memberId);
        if (!validation.success()) {
            auditLogService.logOpsAudit("DEV_TEAM_MEMBER_UPDATE", currentUser, null, developerTarget(currentUser), false, validation.message(), requestUri);
            return Result.error(validation.message());
        }
        member.setMemberName(validation.memberName());
        member.setMemberEmail(validation.memberEmail());
        member.setTeamRole(validation.teamRole());
        member.setMemberStatus(validation.memberStatus());
        member.setNote(validation.note());
        DeveloperTeamMember saved = developerTeamMemberRepository.save(member);
        auditLogService.logOpsAudit("DEV_TEAM_MEMBER_UPDATE", currentUser, null, developerTarget(currentUser), true, saved.getMemberEmail(), requestUri);
        return Result.success(toTeamMemberDto(saved));
    }

    @Transactional
    public Result<DeveloperTeamMemberDto> updateTeamMemberStatus(
            User currentUser,
            Long memberId,
            DeveloperTeamMemberStatusRequest request,
            String requestUri
    ) {
        DeveloperTeamMember member = developerTeamMemberRepository.findByIdAndOwnerUserId(memberId, currentUser.getId()).orElse(null);
        if (member == null) {
            auditLogService.logOpsAudit("DEV_TEAM_MEMBER_STATUS_UPDATE", currentUser, null, developerTarget(currentUser), false, "Team member not found", requestUri);
            return Result.error("Team member not found");
        }
        String status = normalizeEnum(request == null ? null : request.memberStatus(), TEAM_STATUSES);
        if (status == null) {
            auditLogService.logOpsAudit("DEV_TEAM_MEMBER_STATUS_UPDATE", currentUser, null, developerTarget(currentUser), false, "Invalid team member status", requestUri);
            return Result.error("Invalid team member status");
        }
        member.setMemberStatus(status);
        DeveloperTeamMember saved = developerTeamMemberRepository.save(member);
        auditLogService.logOpsAudit("DEV_TEAM_MEMBER_STATUS_UPDATE", currentUser, null, developerTarget(currentUser), true, status, requestUri);
        return Result.success(toTeamMemberDto(saved));
    }

    public Result<List<DeveloperApiKeyDto>> listApiKeys(User currentUser) {
        return Result.success(
                developerApiKeyRepository.findByOwnerUserIdOrderByUpdatedAtDesc(currentUser.getId())
                        .stream()
                        .map(this::toApiKeyDto)
                        .toList()
        );
    }

    public Result<List<DeveloperWorkspaceAuditLogDto>> listWorkspaceAuditLogs(User currentUser) {
        return Result.success(
                auditLogService.searchLogs(null, null, currentUser.getId(), developerTarget(currentUser), 100)
                        .stream()
                        .filter(item -> item.action() != null && (item.action().startsWith("DEV_TEAM_") || item.action().startsWith("DEV_API_KEY_")))
                        .map(item -> new DeveloperWorkspaceAuditLogDto(
                                item.id(),
                                item.action(),
                                item.success(),
                                item.reason(),
                                item.requestUri(),
                                item.createdAt()
                        ))
                        .toList()
        );
    }

    @Transactional
    public Result<DeveloperApiKeyCreateResponse> createApiKey(
            User currentUser,
            DeveloperApiKeyCreateRequest request,
            String requestUri
    ) {
        String keyName = trim(request == null ? null : request.keyName(), 64);
        if (keyName == null || keyName.length() < 2) {
            auditLogService.logOpsAudit("DEV_API_KEY_CREATE", currentUser, null, developerTarget(currentUser), false, "Key name must be at least 2 characters", requestUri);
            return Result.error("Key name must be at least 2 characters");
        }
        List<String> scopes = normalizeScopes(request == null ? null : request.scopes());
        if (scopes.isEmpty()) {
            auditLogService.logOpsAudit("DEV_API_KEY_CREATE", currentUser, null, developerTarget(currentUser), false, "Select at least one scope", requestUri);
            return Result.error("Select at least one scope");
        }
        LocalDateTime expiresAt = request == null ? null : request.expiresAt();
        if (expiresAt != null) {
            if (expiresAt.isBefore(LocalDateTime.now().plusHours(1))) {
                auditLogService.logOpsAudit("DEV_API_KEY_CREATE", currentUser, null, developerTarget(currentUser), false, "Expiration must be at least 1 hour later", requestUri);
                return Result.error("Expiration must be at least 1 hour later");
            }
            if (expiresAt.isAfter(LocalDateTime.now().plusDays(365))) {
                auditLogService.logOpsAudit("DEV_API_KEY_CREATE", currentUser, null, developerTarget(currentUser), false, "Expiration cannot exceed 365 days", requestUri);
                return Result.error("Expiration cannot exceed 365 days");
            }
        }

        String accessKey = generateAccessKey();
        while (developerApiKeyRepository.existsByAccessKey(accessKey)) {
            accessKey = generateAccessKey();
        }
        String secret = generateSecret();
        DeveloperApiKey apiKey = new DeveloperApiKey();
        apiKey.setOwnerUserId(currentUser.getId());
        apiKey.setKeyName(keyName);
        apiKey.setAccessKey(accessKey);
        apiKey.setSecretDigest(digest(secret));
        apiKey.setScopeCodes(String.join(",", scopes));
        apiKey.setStatus(expiresAt != null && expiresAt.isBefore(LocalDateTime.now()) ? "EXPIRED" : "ACTIVE");
        apiKey.setExpiresAt(expiresAt);
        DeveloperApiKey saved = developerApiKeyRepository.save(apiKey);
        auditLogService.logOpsAudit("DEV_API_KEY_CREATE", currentUser, null, developerTarget(currentUser), true, saved.getAccessKey(), requestUri);
        return Result.success(new DeveloperApiKeyCreateResponse(toApiKeyDto(saved), secret));
    }

    @Transactional
    public Result<DeveloperApiKeyDto> revokeApiKey(
            User currentUser,
            Long keyId,
            DeveloperApiKeyRevokeRequest request,
            String requestUri
    ) {
        DeveloperApiKey apiKey = developerApiKeyRepository.findByIdAndOwnerUserId(keyId, currentUser.getId()).orElse(null);
        if (apiKey == null) {
            auditLogService.logOpsAudit("DEV_API_KEY_REVOKE", currentUser, null, developerTarget(currentUser), false, "API key not found", requestUri);
            return Result.error("API key not found");
        }
        String confirmText = trim(request == null ? null : request.confirmText(), 32);
        if (!REVOKE_CONFIRM_TEXT.equalsIgnoreCase(confirmText)) {
            auditLogService.logOpsAudit("DEV_API_KEY_REVOKE", currentUser, null, developerTarget(currentUser), false, "Confirmation text mismatch", requestUri);
            return Result.error("Confirmation text mismatch");
        }
        if ("REVOKED".equalsIgnoreCase(apiKey.getStatus())) {
            return Result.success(toApiKeyDto(apiKey));
        }
        apiKey.setStatus("REVOKED");
        apiKey.setRevokedAt(LocalDateTime.now());
        DeveloperApiKey saved = developerApiKeyRepository.save(apiKey);
        auditLogService.logOpsAudit("DEV_API_KEY_REVOKE", currentUser, null, developerTarget(currentUser), true, saved.getAccessKey(), requestUri);
        return Result.success(toApiKeyDto(saved));
    }

    @Transactional
    public Result<DeveloperApiKeyCreateResponse> rotateApiKey(
            User currentUser,
            Long keyId,
            DeveloperApiKeyRotateRequest request,
            String requestUri
    ) {
        DeveloperApiKey currentKey = developerApiKeyRepository.findByIdAndOwnerUserId(keyId, currentUser.getId()).orElse(null);
        if (currentKey == null) {
            auditLogService.logOpsAudit("DEV_API_KEY_ROTATE", currentUser, null, developerTarget(currentUser), false, "API key not found", requestUri);
            return Result.error("API key not found");
        }
        String confirmText = trim(request == null ? null : request.confirmText(), 32);
        if (!REVOKE_CONFIRM_TEXT.equalsIgnoreCase(confirmText)) {
            auditLogService.logOpsAudit("DEV_API_KEY_ROTATE", currentUser, null, developerTarget(currentUser), false, "Confirmation text mismatch", requestUri);
            return Result.error("Confirmation text mismatch");
        }
        Result<DeveloperApiKeyCreateResponse> createResult = createApiKey(
                currentUser,
                new DeveloperApiKeyCreateRequest(
                        trim(request == null ? null : request.newKeyName(), 64),
                        request == null ? null : request.scopes(),
                        request == null ? null : request.expiresAt()
                ),
                requestUri
        );
        if (createResult.getCode() != 0 || createResult.getData() == null) {
            return createResult;
        }
        currentKey.setStatus("REVOKED");
        currentKey.setRevokedAt(LocalDateTime.now());
        developerApiKeyRepository.save(currentKey);
        auditLogService.logOpsAudit("DEV_API_KEY_ROTATE", currentUser, null, developerTarget(currentUser), true, currentKey.getAccessKey(), requestUri);
        return createResult;
    }

    private ValidationResult validateTeamMember(Long ownerUserId, DeveloperTeamMemberUpsertRequest request, Long currentId) {
        String memberName = trim(request == null ? null : request.memberName(), 64);
        if (memberName == null || memberName.length() < 2) {
            return ValidationResult.error("Member name must be at least 2 characters");
        }
        String memberEmail = normalizeEmail(request == null ? null : request.memberEmail());
        if (memberEmail == null) {
            return ValidationResult.error("Invalid member email");
        }
        boolean duplicated = currentId == null
                ? developerTeamMemberRepository.existsByOwnerUserIdAndMemberEmailIgnoreCase(ownerUserId, memberEmail)
                : developerTeamMemberRepository.existsByOwnerUserIdAndMemberEmailIgnoreCaseAndIdNot(ownerUserId, memberEmail, currentId);
        if (duplicated) {
            return ValidationResult.error("This email already exists in your team");
        }
        String teamRole = normalizeEnum(request == null ? null : request.teamRole(), TEAM_ROLES);
        if (teamRole == null) {
            return ValidationResult.error("Invalid team role");
        }
        String memberStatus = normalizeEnum(request == null ? null : request.memberStatus(), TEAM_STATUSES);
        if (memberStatus == null) {
            memberStatus = "INVITED";
        }
        String note = trim(request == null ? null : request.note(), 256);
        return ValidationResult.success(memberName, memberEmail, teamRole, memberStatus, note);
    }

    private DeveloperTeamMemberDto toTeamMemberDto(DeveloperTeamMember member) {
        return new DeveloperTeamMemberDto(
                member.getId(),
                member.getMemberName(),
                member.getMemberEmail(),
                member.getTeamRole(),
                member.getMemberStatus(),
                member.getNote(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }

    private DeveloperApiKeyDto toApiKeyDto(DeveloperApiKey apiKey) {
        String status = normalizeApiKeyStatus(apiKey);
        return new DeveloperApiKeyDto(
                apiKey.getId(),
                apiKey.getKeyName(),
                apiKey.getAccessKey(),
                parseScopes(apiKey.getScopeCodes()),
                status,
                apiKey.getExpiresAt(),
                apiKey.getLastUsedAt(),
                apiKey.getRevokedAt(),
                apiKey.getCreatedAt(),
                apiKey.getUpdatedAt()
        );
    }

    private String normalizeApiKeyStatus(DeveloperApiKey apiKey) {
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now()) && !"REVOKED".equalsIgnoreCase(apiKey.getStatus())) {
            return "EXPIRED";
        }
        String normalized = normalizeEnum(apiKey.getStatus(), KEY_STATUSES);
        return normalized == null ? "ACTIVE" : normalized;
    }

    private List<String> normalizeScopes(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String scope : scopes) {
            String value = normalizeEnum(scope, KEY_SCOPES);
            if (value != null) {
                normalized.add(value);
            }
        }
        return new ArrayList<>(normalized);
    }

    private List<String> parseScopes(String scopeCodes) {
        if (scopeCodes == null || scopeCodes.isBlank()) {
            return List.of();
        }
        return List.of(scopeCodes.split(","));
    }

    private String normalizeEmail(String email) {
        String value = trim(email, 128);
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            return null;
        }
        return normalized;
    }

    private String normalizeEnum(String value, Set<String> allowed) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return allowed.contains(normalized) ? normalized : null;
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private String digest(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not supported", exception);
        }
    }

    private String generateAccessKey() {
        return "npk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private String generateSecret() {
        return "nps_" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String developerTarget(User currentUser) {
        return "DEV:" + currentUser.getId();
    }

    private record ValidationResult(
            boolean success,
            String message,
            String memberName,
            String memberEmail,
            String teamRole,
            String memberStatus,
            String note
    ) {
        static ValidationResult error(String message) {
            return new ValidationResult(false, message, null, null, null, null, null);
        }

        static ValidationResult success(
                String memberName,
                String memberEmail,
                String teamRole,
                String memberStatus,
                String note
        ) {
            return new ValidationResult(true, null, memberName, memberEmail, teamRole, memberStatus, note);
        }
    }
}
