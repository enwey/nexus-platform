package com.nexus.platform.service;

import com.nexus.platform.dto.BillingDetailDto;
import com.nexus.platform.dto.BillingRecordDto;
import com.nexus.platform.entity.DeveloperCertificationProfile;
import com.nexus.platform.dto.DeviceSessionDto;
import com.nexus.platform.dto.DeveloperGovernanceRecordDto;
import com.nexus.platform.dto.OpsDeveloperAccountDto;
import com.nexus.platform.dto.OpsDeveloperGovernanceUpdateRequest;
import com.nexus.platform.dto.ReferralRecordDto;
import com.nexus.platform.dto.ReferralRecordsResponse;
import com.nexus.platform.dto.ReferralSummaryDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.dto.VerificationCodeResponse;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.DeveloperGovernanceRecord;
import com.nexus.platform.entity.User;
import com.nexus.platform.entity.UserGameActionLog;
import com.nexus.platform.entity.VerificationCodeLog;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.DeveloperCertificationProfileRepository;
import com.nexus.platform.repository.DeveloperGovernanceRecordRepository;
import com.nexus.platform.repository.UserGameActionLogRepository;
import com.nexus.platform.repository.UserRepository;
import com.nexus.platform.repository.VerificationCodeLogRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountOpsService {
    private static final String CODE_PURPOSE_REGISTER = "REGISTER";
    private static final String CODE_PURPOSE_RESET = "RESET_PASSWORD";
    private static final String CODE_PURPOSE_CHANGE_PASSWORD = "CHANGE_PASSWORD";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final DeveloperCertificationProfileRepository developerCertificationProfileRepository;
    private final DeveloperGovernanceRecordRepository developerGovernanceRecordRepository;
    private final UserGameActionLogRepository actionLogRepository;
    private final VerificationCodeLogRepository verificationCodeLogRepository;
    private final OpsAdminDataScopeService opsAdminDataScopeService;
    private final AuthTokenService authTokenService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeEmailService verificationCodeEmailService;
    private final ObjectMapper objectMapper;

    @Value("${platform.security.allow-insecure-defaults:false}")
    private boolean allowInsecureDefaults;

    public Result<VerificationCodeResponse> sendCode(String email, String purpose, VerificationCodeIssueContext context) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            Result<VerificationCodeResponse> result = Result.error("Email is required");
            recordVerificationCodeLog(email, normalizePurpose(purpose), null, context, false, result.getMessage());
            return result;
        }
        String normalizedPurpose = normalizePurpose(purpose);
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            Result<VerificationCodeResponse> result = Result.error("Invalid email format");
            recordVerificationCodeLog(normalizedEmail, normalizedPurpose, null, context, false, result.getMessage());
            return result;
        }
        boolean emailExists = userRepository.existsByEmailIgnoreCase(normalizedEmail);
        if (CODE_PURPOSE_REGISTER.equals(normalizedPurpose) && emailExists) {
            Result<VerificationCodeResponse> result = Result.error("Email already registered");
            recordVerificationCodeLog(normalizedEmail, normalizedPurpose, null, context, false, result.getMessage());
            return result;
        }
        if (!CODE_PURPOSE_REGISTER.equals(normalizedPurpose) && !emailExists) {
            Result<VerificationCodeResponse> result = Result.error("Email is not registered");
            recordVerificationCodeLog(normalizedEmail, normalizedPurpose, null, context, false, result.getMessage());
            return result;
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        String verificationKey = codeKey(normalizedPurpose, normalizedEmail);
        redisTemplate.opsForValue().set(verificationKey, code, java.time.Duration.ofMinutes(5));

        String debugCode = null;
        try {
            if (verificationCodeEmailService.isEnabled()) {
                verificationCodeEmailService.sendVerificationCode(normalizedEmail, normalizedPurpose, code);
            } else if (allowInsecureDefaults) {
                debugCode = code;
                log.warn("Email delivery is disabled. Returning debug verification code in relaxed mode for {}", normalizedEmail);
            } else {
                redisTemplate.delete(verificationKey);
                Result<VerificationCodeResponse> result = Result.error("Email delivery is unavailable");
                recordVerificationCodeLog(normalizedEmail, normalizedPurpose, null, context, false, result.getMessage());
                return result;
            }
        } catch (Exception e) {
            redisTemplate.delete(verificationKey);
            Result<VerificationCodeResponse> result = Result.error("Failed to send verification email");
            recordVerificationCodeLog(normalizedEmail, normalizedPurpose, null, context, false, result.getMessage());
            return result;
        }

        Result<VerificationCodeResponse> result = Result.success(
                new VerificationCodeResponse(normalizedEmail, normalizedPurpose, 300, debugCode)
        );
        recordVerificationCodeLog(normalizedEmail, normalizedPurpose, debugCode, context, true, null);
        return result;
    }

    public Result<List<VerificationCodeLog>> listVerificationCodeLogs(String email, String purpose, String source, int limit) {
        int normalizedLimit = limit <= 0 || limit > 500 ? 100 : limit;
        String normalizedEmail = normalizeEmail(email);
        String normalizedPurpose = normalizePurposeFilter(purpose);
        String normalizedSource = normalizeFilter(source);
        List<VerificationCodeLog> logs = verificationCodeLogRepository.findRecent(
                normalizedEmail,
                normalizedPurpose,
                normalizedSource,
                PageRequest.of(0, normalizedLimit, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return Result.success(logs);
    }

    public Result<List<OpsDeveloperAccountDto>> listDeveloperAccounts(User currentUser) {
        List<User> developers = userRepository.findByRoleOrderByCreatedAtDesc(User.UserRole.DEVELOPER);
        Map<Long, List<Game>> gamesByDeveloper = gameRepository.findAll().stream()
                .filter(game -> game.getDeveloperId() != null)
                .collect(java.util.stream.Collectors.groupingBy(Game::getDeveloperId));
        Map<Long, DeveloperCertificationProfile> certificationByDeveloper = developerCertificationProfileRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(DeveloperCertificationProfile::getDeveloperId, profile -> profile));
        String scopeCode = opsAdminDataScopeService.resolveScopeForUser(currentUser);

        List<OpsDeveloperAccountDto> rows = developers.stream()
                .map(user -> {
                    DeveloperCertificationProfile certificationProfile = certificationByDeveloper.get(user.getId());
                    List<Game> games = gamesByDeveloper.getOrDefault(user.getId(), List.of());
                    int approved = 0;
                    int pending = 0;
                    int rejected = 0;
                    LocalDateTime lastGameAt = null;
                    List<String> gameNames = new ArrayList<>();

                    for (Game game : games) {
                        if (game.getStatus() == Game.GameStatus.APPROVED) {
                            approved += 1;
                        } else if (game.getStatus() == Game.GameStatus.PENDING || game.getStatus() == Game.GameStatus.PROCESSING || game.getStatus() == Game.GameStatus.DRAFT) {
                            pending += 1;
                        } else if (game.getStatus() == Game.GameStatus.REJECTED) {
                            rejected += 1;
                        }
                        if (game.getName() != null && !game.getName().isBlank()) {
                            gameNames.add(game.getName());
                        }
                        if (game.getCreatedAt() != null && (lastGameAt == null || game.getCreatedAt().isAfter(lastGameAt))) {
                            lastGameAt = game.getCreatedAt();
                        }
                    }

                    return new OpsDeveloperAccountDto(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getRole().name(),
                            normalizeAccountStatus(user.getAccountStatus()),
                            normalizeCertificationStatus(user.getCertificationStatus()),
                            normalizeCertificationProfileStatus(certificationProfile == null ? null : certificationProfile.getProfileStatus()),
                            certificationProfile == null ? null : certificationProfile.getSubjectType(),
                            certificationProfile == null ? null : certificationProfile.getSubjectName(),
                            normalizeRiskLevel(user.getRiskLevel()),
                            normalizeWhitelistStatus(user.getWhitelistStatus()),
                            safeViolationCount(user.getViolationCount()),
                            user.getGovernanceTag(),
                            user.getOpsNote(),
                            user.getCreatedAt(),
                            games.size(),
                            approved,
                            pending,
                            rejected,
                            lastGameAt,
                            gameNames.stream().distinct().sorted().toList()
                    );
                })
                .filter(row -> matchesAdminScope(row, scopeCode))
                .sorted(Comparator
                        .comparingInt(OpsDeveloperAccountDto::gameCount).reversed()
                        .thenComparing(OpsDeveloperAccountDto::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return Result.success(rows);
    }

    public Result<List<DeveloperGovernanceRecordDto>> listDeveloperGovernanceRecords(Long developerId) {
        if (developerId == null || developerId <= 0) {
            return Result.error("Invalid developer id");
        }
        return Result.success(
                developerGovernanceRecordRepository.findByDeveloperIdOrderByCreatedAtDesc(developerId)
                        .stream()
                        .map(record -> new DeveloperGovernanceRecordDto(
                                record.getId(),
                                record.getDeveloperId(),
                                record.getOperatorId(),
                                record.getActionType(),
                                record.getReason(),
                                record.getBeforeSnapshotJson(),
                                record.getAfterSnapshotJson(),
                                record.getCreatedAt()
                        ))
                        .toList()
        );
    }

    @Transactional
    public Result<OpsDeveloperAccountDto> updateDeveloperGovernance(
            Long developerId,
            OpsDeveloperGovernanceUpdateRequest request,
            User currentUser,
            String requestUri
    ) {
        if (developerId == null || developerId <= 0) {
            auditLogService.logOpsAudit("DEVELOPER_GOVERNANCE_UPDATE", currentUser, null, null, false, "Invalid developer id", requestUri);
            return Result.error("Invalid developer id");
        }
        User developer = userRepository.findById(developerId).orElse(null);
        if (developer == null || developer.getRole() != User.UserRole.DEVELOPER) {
            auditLogService.logOpsAudit("DEVELOPER_GOVERNANCE_UPDATE", currentUser, null, null, false, "Developer not found", requestUri);
            return Result.error("Developer not found");
        }
        String nextStatus = normalizeAccountStatus(request == null ? null : request.accountStatus());
        if (nextStatus == null) {
            auditLogService.logOpsAudit("DEVELOPER_GOVERNANCE_UPDATE", currentUser, null, null, false, "Invalid account status", requestUri);
            return Result.error("Invalid account status");
        }
        String reason = trimToNull(request == null ? null : request.reason());
        if (!"ACTIVE".equals(nextStatus) && (reason == null || reason.length() < 2)) {
            auditLogService.logOpsAudit("DEVELOPER_GOVERNANCE_UPDATE", currentUser, null, null, false, "Reason must be at least 2 chars", requestUri);
            return Result.error("Reason must be at least 2 chars");
        }
        String governanceTag = trimToNull(request == null ? null : request.governanceTag());
        if (governanceTag != null && governanceTag.length() > 64) {
            auditLogService.logOpsAudit("DEVELOPER_GOVERNANCE_UPDATE", currentUser, null, null, false, "Governance tag is too long", requestUri);
            return Result.error("Governance tag is too long");
        }
        String certificationStatus = normalizeCertificationStatus(request == null ? null : request.certificationStatus());
        if (certificationStatus == null) {
            auditLogService.logOpsAudit("DEVELOPER_GOVERNANCE_UPDATE", currentUser, null, null, false, "Invalid certification status", requestUri);
            return Result.error("Invalid certification status");
        }
        String riskLevel = normalizeRiskLevel(request == null ? null : request.riskLevel());
        if (riskLevel == null) {
            auditLogService.logOpsAudit("DEVELOPER_GOVERNANCE_UPDATE", currentUser, null, null, false, "Invalid risk level", requestUri);
            return Result.error("Invalid risk level");
        }
        String whitelistStatus = normalizeWhitelistStatus(request == null ? null : request.whitelistStatus());
        if (whitelistStatus == null) {
            auditLogService.logOpsAudit("DEVELOPER_GOVERNANCE_UPDATE", currentUser, null, null, false, "Invalid whitelist status", requestUri);
            return Result.error("Invalid whitelist status");
        }
        Integer violationCount = request == null ? 0 : safeViolationCount(request.violationCount());
        if (violationCount < 0 || violationCount > 9999) {
            auditLogService.logOpsAudit("DEVELOPER_GOVERNANCE_UPDATE", currentUser, null, null, false, "Violation count is invalid", requestUri);
            return Result.error("Violation count is invalid");
        }
        String opsNote = trimToNull(request == null ? null : request.opsNote());
        if (opsNote != null && opsNote.length() > 256) {
            auditLogService.logOpsAudit("DEVELOPER_GOVERNANCE_UPDATE", currentUser, null, null, false, "Ops note is too long", requestUri);
            return Result.error("Ops note is too long");
        }

        String beforeSnapshot = buildGovernanceSnapshot(developer);

        developer.setAccountStatus(nextStatus);
        developer.setCertificationStatus(certificationStatus);
        developer.setRiskLevel(riskLevel);
        developer.setWhitelistStatus(whitelistStatus);
        developer.setViolationCount(violationCount);
        developer.setGovernanceTag(governanceTag);
        developer.setOpsNote(opsNote);
        userRepository.save(developer);
        authTokenService.markUserTokensInvalidBefore(developer.getId());
        recordDeveloperGovernance(developer, currentUser, reason, beforeSnapshot, buildGovernanceSnapshot(developer));

        auditLogService.logOpsAudit(
                "DEVELOPER_GOVERNANCE_UPDATE",
                currentUser,
                null,
                null,
                true,
                "Set developer#" + developer.getId() + " status to " + nextStatus + (reason == null ? "" : " (" + reason + ")"),
                requestUri
        );
        return Result.success(buildDeveloperDto(developer));
    }

    @Transactional
    public Result<Void> resetPassword(String email, String code, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || code == null || code.isBlank()) {
            return Result.error("Invalid parameters");
        }
        if (!isStrongPassword(newPassword)) {
            return Result.error("Password must be at least 8 chars and include letters and digits");
        }
        if (!verifyCode(CODE_PURPOSE_RESET, normalizedEmail, code)) {
            return Result.error("Verification code is invalid or expired");
        }

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user == null) {
            return Result.error("User not found");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        redisTemplate.delete(codeKey(CODE_PURPOSE_RESET, normalizedEmail));
        return Result.success();
    }

    @Transactional
    public Result<Void> changePassword(User currentUser, String email, String code, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || code == null || code.isBlank() || newPassword == null || newPassword.isBlank()) {
            return Result.error("Invalid parameters");
        }
        if (!isStrongPassword(newPassword)) {
            return Result.error("Password must be at least 8 chars and include letters and digits");
        }
        User user = userRepository.findById(currentUser.getId()).orElse(null);
        if (user == null) {
            return Result.error("User not found");
        }
        String currentEmail = normalizeEmail(user.getEmail());
        if (currentEmail == null || !currentEmail.equals(normalizedEmail)) {
            return Result.error("Email does not match current account");
        }
        if (!verifyCode(CODE_PURPOSE_CHANGE_PASSWORD, normalizedEmail, code)) {
            return Result.error("Verification code is invalid or expired");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        redisTemplate.delete(codeKey(CODE_PURPOSE_CHANGE_PASSWORD, normalizedEmail));
        return Result.success();
    }

    public Result<Void> verifyRegisterCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || code == null || code.isBlank()) {
            return Result.error("Invalid parameters");
        }
        if (!verifyCode(CODE_PURPOSE_REGISTER, normalizedEmail, code)) {
            return Result.error("Verification code is invalid or expired");
        }
        redisTemplate.delete(codeKey(CODE_PURPOSE_REGISTER, normalizedEmail));
        return Result.success();
    }

    public Result<List<DeviceSessionDto>> listDevices(Long userId, String currentDeviceId) {
        List<DeviceSessionDto> devices = loadDevices(userId, currentDeviceId);
        if (devices.isEmpty()) {
            String fallbackDeviceId = currentDeviceId == null ? "device-" + UUID.randomUUID() : currentDeviceId;
            DeviceSessionDto fallback = new DeviceSessionDto(
                    fallbackDeviceId,
                    "Current Device",
                    "Android Device",
                    "127.0.0.1",
                    LocalDateTime.now(),
                    true
            );
            saveDevice(userId, fallback);
            devices = List.of(fallback);
        }
        return Result.success(devices);
    }

    public void recordDeviceLogin(User user, String clientIp, String deviceId) {
        if (user == null || user.getId() == null || deviceId == null || deviceId.isBlank()) {
            return;
        }
        DeviceSessionDto device = new DeviceSessionDto(
                deviceId,
                "Android Device",
                "Android",
                Optional.ofNullable(clientIp).orElse("unknown"),
                LocalDateTime.now(),
                false
        );
        saveDevice(user.getId(), device);
    }

    @Transactional
    public Result<Void> kickDevice(Long userId, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return Result.error("deviceId is required");
        }
        redisTemplate.delete(deviceHashKey(userId, deviceId));
        redisTemplate.opsForZSet().remove(deviceIndexKey(userId), deviceId);
        return Result.success();
    }

    @Transactional
    public Result<Void> logoutAll(Long userId, String currentDeviceId) {
        List<DeviceSessionDto> devices = loadDevices(userId, currentDeviceId);
        for (DeviceSessionDto device : devices) {
            if (!device.deviceId().equals(currentDeviceId)) {
                redisTemplate.delete(deviceHashKey(userId, device.deviceId()));
                redisTemplate.opsForZSet().remove(deviceIndexKey(userId), device.deviceId());
            }
        }
        authTokenService.markUserTokensInvalidBefore(userId);
        return Result.success();
    }

    public boolean isAccountOperable(User user) {
        return user != null && "ACTIVE".equals(normalizeAccountStatus(user.getAccountStatus()));
    }

    public String getAccountBlockReason(User user) {
        if (user == null) {
            return "Account is unavailable";
        }
        String status = normalizeAccountStatus(user.getAccountStatus());
        return switch (status) {
            case "SUSPENDED" -> "Account is suspended by platform governance";
            case "BANNED" -> "Account is banned by platform governance";
            default -> null;
        };
    }

    private OpsDeveloperAccountDto buildDeveloperDto(User user) {
        List<Game> games = gameRepository.findByDeveloperIdOrderByCreatedAtDesc(user.getId());
        DeveloperCertificationProfile certificationProfile = developerCertificationProfileRepository.findByDeveloperId(user.getId()).orElse(null);
        int approved = 0;
        int pending = 0;
        int rejected = 0;
        LocalDateTime lastGameAt = null;
        List<String> gameNames = new ArrayList<>();
        for (Game game : games) {
            if (game.getStatus() == Game.GameStatus.APPROVED) approved += 1;
            else if (game.getStatus() == Game.GameStatus.REJECTED) rejected += 1;
            else pending += 1;
            if (game.getName() != null && !game.getName().isBlank()) gameNames.add(game.getName());
            if (game.getCreatedAt() != null && (lastGameAt == null || game.getCreatedAt().isAfter(lastGameAt))) {
                lastGameAt = game.getCreatedAt();
            }
        }
        return new OpsDeveloperAccountDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                normalizeAccountStatus(user.getAccountStatus()),
                normalizeCertificationStatus(user.getCertificationStatus()),
                normalizeCertificationProfileStatus(certificationProfile == null ? null : certificationProfile.getProfileStatus()),
                certificationProfile == null ? null : certificationProfile.getSubjectType(),
                certificationProfile == null ? null : certificationProfile.getSubjectName(),
                normalizeRiskLevel(user.getRiskLevel()),
                normalizeWhitelistStatus(user.getWhitelistStatus()),
                safeViolationCount(user.getViolationCount()),
                user.getGovernanceTag(),
                user.getOpsNote(),
                user.getCreatedAt(),
                games.size(),
                approved,
                pending,
                rejected,
                lastGameAt,
                gameNames.stream().distinct().sorted().toList()
        );
    }

    private void recordDeveloperGovernance(User developer, User operator, String reason, String beforeSnapshot, String afterSnapshot) {
        DeveloperGovernanceRecord record = new DeveloperGovernanceRecord();
        record.setDeveloperId(developer.getId());
        record.setOperatorId(operator == null ? null : operator.getId());
        record.setActionType("ACCOUNT_GOVERNANCE_UPDATE");
        record.setReason(reason);
        record.setBeforeSnapshotJson(beforeSnapshot);
        record.setAfterSnapshotJson(afterSnapshot);
        developerGovernanceRecordRepository.save(record);
    }

    private String buildGovernanceSnapshot(User developer) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("accountStatus", normalizeAccountStatus(developer.getAccountStatus()));
        snapshot.put("certificationStatus", normalizeCertificationStatus(developer.getCertificationStatus()));
        snapshot.put("riskLevel", normalizeRiskLevel(developer.getRiskLevel()));
        snapshot.put("whitelistStatus", normalizeWhitelistStatus(developer.getWhitelistStatus()));
        snapshot.put("violationCount", safeViolationCount(developer.getViolationCount()));
        snapshot.put("governanceTag", developer.getGovernanceTag());
        snapshot.put("opsNote", developer.getOpsNote());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    @Transactional
    public Result<Void> terminateAccount(User currentUser, String confirmText) {
        if (!"确认注销".equals(confirmText) && !"確認註銷".equals(confirmText)) {
            return Result.error("Confirm text mismatch");
        }
        User user = userRepository.findById(currentUser.getId()).orElse(null);
        if (user == null) {
            return Result.error("User not found");
        }
        String suffix = "_deleted_" + System.currentTimeMillis();
        user.setUsername(user.getUsername() + suffix);
        user.setEmail(null);
        user.setPhone(null);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        userRepository.save(user);
        authTokenService.markUserTokensInvalidBefore(currentUser.getId());
        return Result.success();
    }

    public Result<List<BillingRecordDto>> getBillingList(Long userId, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        List<UserGameActionLog> logs = actionLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<BillingRecordDto> records = new ArrayList<>();
        for (UserGameActionLog log : logs) {
            records.add(toBillingRecord(log));
            if (records.size() >= normalizedLimit) {
                break;
            }
        }
        return Result.success(records);
    }

    public Result<BillingDetailDto> getBillingDetail(Long userId, Long id) {
        UserGameActionLog log = actionLogRepository.findById(id).orElse(null);
        if (log == null || !userId.equals(log.getUserId())) {
            return Result.error("Billing record not found");
        }
        BillingRecordDto base = toBillingRecord(log);
        BillingDetailDto detail = new BillingDetailDto(
                base.id(),
                base.type(),
                base.title(),
                base.subtitle(),
                base.amount(),
                base.createdAt(),
                "/wallet/billing/" + base.id() + "/receipt"
        );
        return Result.success(detail);
    }

    public Result<ReferralSummaryDto> getReferralSummary(Long userId) {
        List<UserGameActionLog> logs = actionLogRepository.findByUserIdAndActionTypeOrderByCreatedAtDesc(userId, "REFERRAL_SHARE");
        long inviteCount = logs.size();
        BigDecimal totalReward = BigDecimal.valueOf(inviteCount).multiply(BigDecimal.valueOf(500));
        String referralLink = "https://nexus.link/join/user" + userId;
        return Result.success(new ReferralSummaryDto(inviteCount, totalReward, referralLink));
    }

    @Transactional
    public Result<Void> markReferralShared(Long userId, String channel) {
        UserGameActionLog log = new UserGameActionLog();
        log.setUserId(userId);
        log.setActionType("REFERRAL_SHARE");
        log.setScene("PROFILE_REFERRAL");
        log.setPayloadJson("{\"channel\":\"" + (channel == null ? "unknown" : channel) + "\"}");
        actionLogRepository.save(log);
        return Result.success();
    }

    public Result<ReferralRecordsResponse> getReferralRecords(Long userId, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        List<UserGameActionLog> logs = actionLogRepository.findByUserIdAndActionTypeOrderByCreatedAtDesc(userId, "REFERRAL_SHARE");
        List<ReferralRecordDto> records = logs.stream()
                .limit(normalizedLimit)
                .map(log -> new ReferralRecordDto(
                        log.getId(),
                        "Invite share",
                        "Shared referral link",
                        BigDecimal.valueOf(500),
                        log.getCreatedAt()
                ))
                .toList();
        ReferralSummaryDto summary = getReferralSummary(userId).getData();
        return Result.success(new ReferralRecordsResponse(summary, records));
    }

    private BillingRecordDto toBillingRecord(UserGameActionLog log) {
        BigDecimal amount;
        String type = Optional.ofNullable(log.getActionType()).orElse("UNKNOWN");
        String title;
        String subtitle = "action: " + type;
        switch (type) {
            case "SHARE" -> {
                amount = BigDecimal.valueOf(50);
                title = "Game share reward";
            }
            case "FAVORITE" -> {
                amount = BigDecimal.valueOf(10);
                title = "Favorite bonus";
            }
            case "UNFAVORITE" -> {
                amount = BigDecimal.valueOf(-10);
                title = "Favorite rollback";
            }
            default -> {
                amount = BigDecimal.valueOf(-30);
                title = "Game play consume";
            }
        }
        return new BillingRecordDto(
                log.getId(),
                type,
                title,
                subtitle,
                amount,
                log.getCreatedAt()
        );
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String value = email.trim();
        return value.isEmpty() ? null : value.toLowerCase();
    }

    private String normalizePurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return CODE_PURPOSE_RESET;
        }
        String normalized = purpose.trim().toUpperCase();
        if (CODE_PURPOSE_REGISTER.equals(normalized)) {
            return CODE_PURPOSE_REGISTER;
        }
        if (CODE_PURPOSE_CHANGE_PASSWORD.equals(normalized)) {
            return CODE_PURPOSE_CHANGE_PASSWORD;
        }
        return CODE_PURPOSE_RESET;
    }

    private String normalizeAccountStatus(String value) {
        if (value == null || value.isBlank()) {
            return "ACTIVE";
        }
        String normalized = value.trim().toUpperCase();
        if ("ACTIVE".equals(normalized) || "SUSPENDED".equals(normalized) || "BANNED".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String normalizeCertificationStatus(String value) {
        if (value == null || value.isBlank()) {
            return "UNVERIFIED";
        }
        String normalized = value.trim().toUpperCase();
        if ("UNVERIFIED".equals(normalized) || "PENDING".equals(normalized) || "VERIFIED".equals(normalized) || "REJECTED".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String normalizeCertificationProfileStatus(String value) {
        if (value == null || value.isBlank()) {
            return "DRAFT";
        }
        String normalized = value.trim().toUpperCase();
        if ("DRAFT".equals(normalized) || "PENDING".equals(normalized) || "VERIFIED".equals(normalized) || "REJECTED".equals(normalized)) {
            return normalized;
        }
        if ("UNVERIFIED".equals(normalized)) {
            return "DRAFT";
        }
        return null;
    }

    private String normalizeRiskLevel(String value) {
        if (value == null || value.isBlank()) {
            return "NORMAL";
        }
        String normalized = value.trim().toUpperCase();
        if ("LOW".equals(normalized) || "NORMAL".equals(normalized) || "HIGH".equals(normalized) || "CRITICAL".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String normalizeWhitelistStatus(String value) {
        if (value == null || value.isBlank()) {
            return "STANDARD";
        }
        String normalized = value.trim().toUpperCase();
        if ("STANDARD".equals(normalized) || "WHITELISTED".equals(normalized) || "BLACKLISTED".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private Integer safeViolationCount(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizePurposeFilter(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return null;
        }
        return normalizePurpose(purpose);
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean matchesAdminScope(OpsDeveloperAccountDto row, String scopeCode) {
        if (scopeCode == null || OpsAdminDataScopeService.ALL_DEVELOPERS.equals(scopeCode)) {
            return true;
        }
        if (OpsAdminDataScopeService.HIGH_RISK_ONLY.equals(scopeCode)) {
            return "HIGH".equals(row.riskLevel()) || "CRITICAL".equals(row.riskLevel()) || "BLACKLISTED".equals(row.whitelistStatus());
        }
        if (OpsAdminDataScopeService.PENDING_CERT_ONLY.equals(scopeCode)) {
            return "PENDING".equals(row.certificationStatus()) || "PENDING".equals(row.certificationProfileStatus());
        }
        if (OpsAdminDataScopeService.BLOCKED_ONLY.equals(scopeCode)) {
            return "SUSPENDED".equals(row.accountStatus()) || "BANNED".equals(row.accountStatus());
        }
        return false;
    }

    private void recordVerificationCodeLog(
            String account,
            String purpose,
            String code,
            VerificationCodeIssueContext context,
            boolean success,
            String failureReason
    ) {
        VerificationCodeLog log = new VerificationCodeLog();
        log.setAccount(normalizeEmail(account) == null ? "unknown" : normalizeEmail(account));
        log.setPurpose(normalizePurpose(purpose));
        log.setDebugCode(code);
        log.setSuccess(success);
        log.setFailureReason(truncate(failureReason, 256));

        if (context != null) {
            if (context.requester() != null) {
                log.setRequesterUserId(context.requester().getId());
                log.setRequesterRole(context.requester().getRole() == null ? null : context.requester().getRole().name());
            }
            log.setRequestIp(truncate(context.requestIp(), 64));
            log.setRequestUri(truncate(context.requestUri(), 256));
            log.setRequestSource(truncate(detectSource(context.requestSource(), context.userAgent()), 64));
            log.setRequestScene(truncate(context.requestScene(), 64));
            log.setUserAgent(truncate(context.userAgent(), 512));
        }
        verificationCodeLogRepository.save(log);
    }

    private String detectSource(String source, String userAgent) {
        String normalized = normalizeFilter(source);
        if (normalized != null) {
            return normalized;
        }
        String ua = userAgent == null ? "" : userAgent.toLowerCase();
        if (ua.contains("okhttp") || ua.contains("android")) {
            return "android-app";
        }
        if (ua.contains("mozilla") || ua.contains("chrome") || ua.contains("safari")) {
            return "web-browser";
        }
        return "unknown";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public record VerificationCodeIssueContext(
            User requester,
            String requestIp,
            String requestUri,
            String requestSource,
            String requestScene,
            String userAgent
    ) {}

    private boolean verifyCode(String purpose, String account, String code) {
        String expected = redisTemplate.opsForValue().get(codeKey(purpose, account));
        return expected != null && expected.equals(code.trim());
    }

    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            }
            if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }

    private String codeKey(String purpose, String account) {
        return "auth:verify:" + purpose + ":" + account;
    }

    private String deviceIndexKey(Long userId) {
        return "user:devices:index:" + userId;
    }

    private String deviceHashKey(Long userId, String deviceId) {
        return "user:device:" + userId + ":" + deviceId;
    }

    private void saveDevice(Long userId, DeviceSessionDto dto) {
        String key = deviceHashKey(userId, dto.deviceId());
        Map<String, String> hash = new HashMap<>();
        hash.put("deviceId", dto.deviceId());
        hash.put("deviceName", dto.deviceName());
        hash.put("model", dto.model());
        hash.put("ip", dto.ip());
        hash.put("lastActiveAt", dto.lastActiveAt().toString());
        redisTemplate.opsForHash().putAll(key, hash);
        redisTemplate.expire(key, java.time.Duration.ofDays(30));
        redisTemplate.opsForZSet().add(deviceIndexKey(userId), dto.deviceId(), System.currentTimeMillis());
        redisTemplate.expire(deviceIndexKey(userId), java.time.Duration.ofDays(30));
    }

    private List<DeviceSessionDto> loadDevices(Long userId, String currentDeviceId) {
        var zset = redisTemplate.opsForZSet().reverseRange(deviceIndexKey(userId), 0, 20);
        if (zset == null || zset.isEmpty()) {
            return List.of();
        }
        List<DeviceSessionDto> devices = new ArrayList<>();
        for (String deviceId : zset) {
            Map<Object, Object> hash = redisTemplate.opsForHash().entries(deviceHashKey(userId, deviceId));
            if (hash == null || hash.isEmpty()) {
                continue;
            }
            LocalDateTime lastActive = LocalDateTime.parse(String.valueOf(hash.getOrDefault("lastActiveAt", LocalDateTime.now().toString())));
            devices.add(new DeviceSessionDto(
                    String.valueOf(hash.getOrDefault("deviceId", deviceId)),
                    String.valueOf(hash.getOrDefault("deviceName", "Android Device")),
                    String.valueOf(hash.getOrDefault("model", "Android")),
                    String.valueOf(hash.getOrDefault("ip", "unknown")),
                    lastActive,
                    deviceId.equals(currentDeviceId)
            ));
        }
        devices.sort(Comparator.comparing(DeviceSessionDto::lastActiveAt).reversed());
        return devices;
    }
}
