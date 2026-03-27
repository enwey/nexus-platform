package com.nexus.platform.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginSecurityService {
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate stringRedisTemplate;

    public String getBlockReason(String username, String clientIp) {
        String key = buildKey(username, clientIp);
        Long seconds = stringRedisTemplate.getExpire(lockKey(key));
        if (seconds == null || seconds < 0) {
            return null;
        }
        return "鐧诲綍澶辫触娆℃暟杩囧锛岃 " + Math.max(seconds, 1) + " 绉掑悗閲嶈瘯";
    }

    public void onLoginFailed(String username, String clientIp) {
        String key = buildKey(username, clientIp);
        String failureKey = failureKey(key);
        Long failures = stringRedisTemplate.opsForValue().increment(failureKey);
        if (failures != null && failures == 1L) {
            stringRedisTemplate.expire(failureKey, FAILURE_WINDOW);
        }
        if (failures != null && failures >= MAX_FAILURES) {
            stringRedisTemplate.opsForValue().set(lockKey(key), "1", LOCK_DURATION);
            stringRedisTemplate.delete(failureKey);
        }
    }

    public void onLoginSuccess(String username, String clientIp) {
        String key = buildKey(username, clientIp);
        stringRedisTemplate.delete(failureKey(key));
        stringRedisTemplate.delete(lockKey(key));
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
