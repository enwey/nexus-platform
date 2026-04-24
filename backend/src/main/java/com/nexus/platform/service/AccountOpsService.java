package com.nexus.platform.service;

import com.nexus.platform.dto.BillingDetailDto;
import com.nexus.platform.dto.BillingRecordDto;
import com.nexus.platform.dto.DeviceSessionDto;
import com.nexus.platform.dto.ReferralRecordDto;
import com.nexus.platform.dto.ReferralRecordsResponse;
import com.nexus.platform.dto.ReferralSummaryDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.dto.VerificationCodeResponse;
import com.nexus.platform.entity.User;
import com.nexus.platform.entity.UserGameActionLog;
import com.nexus.platform.entity.VerificationCodeLog;
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
    private final UserGameActionLogRepository actionLogRepository;
    private final VerificationCodeLogRepository verificationCodeLogRepository;
    private final AuthTokenService authTokenService;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeEmailService verificationCodeEmailService;

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
