package com.nexus.platform.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class LoginSecurityService {
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public String getBlockReason(String username, String clientIp) {
        String key = buildKey(username, clientIp);
        AttemptState state = attempts.get(key);
        if (state == null) {
            return null;
        }
        if (state.lockUntil() != null && state.lockUntil().isAfter(Instant.now())) {
            long seconds = Duration.between(Instant.now(), state.lockUntil()).getSeconds();
            return "登录失败次数过多，请 " + Math.max(seconds, 1) + " 秒后重试";
        }
        if (state.lockUntil() != null && state.lockUntil().isBefore(Instant.now())) {
            attempts.remove(key);
        }
        return null;
    }

    public void onLoginFailed(String username, String clientIp) {
        String key = buildKey(username, clientIp);
        attempts.compute(key, (k, old) -> {
            int failures = old == null ? 1 : old.failures() + 1;
            Instant lockUntil = failures >= MAX_FAILURES ? Instant.now().plus(LOCK_DURATION) : null;
            return new AttemptState(failures, lockUntil);
        });
    }

    public void onLoginSuccess(String username, String clientIp) {
        attempts.remove(buildKey(username, clientIp));
    }

    private String buildKey(String username, String clientIp) {
        return (username == null ? "" : username.trim().toLowerCase()) + "|" + (clientIp == null ? "unknown" : clientIp);
    }

    private record AttemptState(int failures, Instant lockUntil) {
    }
}

