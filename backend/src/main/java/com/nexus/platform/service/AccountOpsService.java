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
import com.nexus.platform.repository.UserGameActionLogRepository;
import com.nexus.platform.repository.UserRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountOpsService {
    private static final String CODE_PURPOSE_REGISTER = "REGISTER";
    private static final String CODE_PURPOSE_RESET = "RESET_PASSWORD";

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final UserGameActionLogRepository actionLogRepository;
    private final AuthTokenService authTokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Result<VerificationCodeResponse> sendCode(String account, String purpose) {
        String normalizedAccount = normalizeAccount(account);
        if (normalizedAccount == null) {
            return Result.error("Account is required");
        }
        String normalizedPurpose = normalizePurpose(purpose);
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        redisTemplate.opsForValue().set(codeKey(normalizedPurpose, normalizedAccount), code, java.time.Duration.ofMinutes(5));
        return Result.success(new VerificationCodeResponse(normalizedAccount, normalizedPurpose, 300, code));
    }

    @Transactional
    public Result<Void> resetPassword(String account, String code, String newPassword) {
        String normalizedAccount = normalizeAccount(account);
        if (normalizedAccount == null || code == null || code.isBlank()) {
            return Result.error("Invalid parameters");
        }
        if (!isStrongPassword(newPassword)) {
            return Result.error("Password must be at least 8 chars and include letters and digits");
        }
        if (!verifyCode(CODE_PURPOSE_RESET, normalizedAccount, code)) {
            return Result.error("Verification code is invalid or expired");
        }

        User user = findUserByAccount(normalizedAccount);
        if (user == null) {
            return Result.error("User not found");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        redisTemplate.delete(codeKey(CODE_PURPOSE_RESET, normalizedAccount));
        return Result.success();
    }

    @Transactional
    public Result<Void> changePassword(User currentUser, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
            return Result.error("Invalid parameters");
        }
        if (!isStrongPassword(newPassword)) {
            return Result.error("Password must be at least 8 chars and include letters and digits");
        }
        User user = userRepository.findById(currentUser.getId()).orElse(null);
        if (user == null) {
            return Result.error("User not found");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
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

    private String normalizeAccount(String account) {
        if (account == null) {
            return null;
        }
        String value = account.trim();
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
        return CODE_PURPOSE_RESET;
    }

    private boolean verifyCode(String purpose, String account, String code) {
        String expected = redisTemplate.opsForValue().get(codeKey(purpose, account));
        return expected != null && expected.equals(code.trim());
    }

    private User findUserByAccount(String account) {
        User byUsername = userRepository.findByUsername(account).orElse(null);
        if (byUsername != null) {
            return byUsername;
        }
        User byEmail = userRepository.findByEmail(account).orElse(null);
        if (byEmail != null) {
            return byEmail;
        }
        return userRepository.findByPhone(account).orElse(null);
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
