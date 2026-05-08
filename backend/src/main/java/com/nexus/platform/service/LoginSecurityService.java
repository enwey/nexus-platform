package com.nexus.platform.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginSecurityService {
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final StringRedisTemplate stringRedisTemplate;
    private final LoginRiskOpsService loginRiskOpsService;

    public String getBlockReason(String username, String clientIp, String deviceId) {
        String accessRuleReason = loginRiskOpsService.resolveAccessBlockReason(clientIp, deviceId);
        if (accessRuleReason != null) {
            return accessRuleReason;
        }
        String key = buildKey(username, clientIp);
        Long seconds = stringRedisTemplate.getExpire(lockKey(key));
        if (seconds == null || seconds < 0) {
            return null;
        }
        return "登录失败次数过多，请 " + Math.max(seconds, 1) + " 秒后重试";
    }

    public void onLoginFailed(String username, String clientIp, String deviceId, String userAgent, String reason) {
        String key = buildKey(username, clientIp);
        String failureKey = failureKey(key);
        int failureThreshold = loginRiskOpsService.getFailureThreshold();
        Duration failureWindow = loginRiskOpsService.getFailureWindow();
        Long failures = stringRedisTemplate.opsForValue().increment(failureKey);
        if (failures != null && failures == 1L) {
            stringRedisTemplate.expire(failureKey, failureWindow);
        }
        String severity = failures != null && failures >= failureThreshold ? loginRiskOpsService.getFailureSeverity() : "MEDIUM";
        loginRiskOpsService.recordEvent(username, null, clientIp, deviceId, userAgent, "LOGIN_FAILED", severity, "FAILED", reason);
        loginRiskOpsService.recordHighFrequencyAlert(username, null, clientIp, deviceId, userAgent);
        if (failures != null && failures >= failureThreshold) {
            stringRedisTemplate.opsForValue().set(lockKey(key), "1", LOCK_DURATION);
            stringRedisTemplate.delete(failureKey);
            loginRiskOpsService.recordEvent(username, null, clientIp, deviceId, userAgent, "LOGIN_LOCKED", loginRiskOpsService.getFailureSeverity(), "FAILED", "Too many login failures");
        }
    }

    public void onLoginSuccess(String username, Long userId, String clientIp, String deviceId, String userAgent) {
        String key = buildKey(username, clientIp);
        stringRedisTemplate.delete(failureKey(key));
        stringRedisTemplate.delete(lockKey(key));
        boolean watched = loginRiskOpsService.isWatched(clientIp, deviceId);
        boolean highFrequency = loginRiskOpsService.isHighFrequencyIp(clientIp);
        if (watched || highFrequency) {
            String reason = watched ? "Matched watch rule" : "High frequency login from same IP";
            String severity = watched ? loginRiskOpsService.getWatchSeverity() : "HIGH";
            loginRiskOpsService.recordEvent(username, userId, clientIp, deviceId, userAgent, "SUSPICIOUS_LOGIN", severity, "SUCCESS", reason);
        } else {
            loginRiskOpsService.recordEvent(username, userId, clientIp, deviceId, userAgent, "LOGIN_SUCCESS", "LOW", "SUCCESS", "Login succeeded");
        }
    }

    private String buildKey(String username, String clientIp) {
        return (username == null ? "" : username.trim().toLowerCase()) + "|" + (clientIp == null ? "unknown" : clientIp);
    }

    private String failureKey(String key) {
        return "auth:login:fail:" + key;
    }

    private String lockKey(String key) {
        return "auth:login:lock:" + key;
    }
}
