package com.nexus.platform.service;

import com.nexus.platform.dto.Result;
import io.nakama.core.DefaultSession;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NakamaService {

    public Result<DefaultSession> authenticate(String userId, String token) {
        log.warn("Nakama authenticate called before social integration is implemented. userId={}", userId);
        return Result.error("社交能力暂未接入");
    }

    public Result<String> register(String username, String password) {
        log.warn("Nakama register called before social integration is implemented. username={}", username);
        return Result.error("社交能力暂未接入");
    }

    public Result<Void> writeStorage(String userId, String key, String value) {
        log.warn("Nakama writeStorage called before social integration is implemented. userId={}, key={}", userId, key);
        return Result.error("社交能力暂未接入");
    }

    public Result<String> readStorage(String userId, String key) {
        log.warn("Nakama readStorage called before social integration is implemented. userId={}, key={}", userId, key);
        return Result.error("社交能力暂未接入");
    }

    public Result<Void> writeLeaderboardRecord(String userId, String leaderboardId, long score, long subscore) {
        log.warn("Nakama writeLeaderboardRecord called before social integration is implemented. userId={}, leaderboardId={}", userId, leaderboardId);
        return Result.error("社交能力暂未接入");
    }

    public Result<java.util.List<io.nakama.api.LeaderboardRecord>> getLeaderboardRecords(String userId, String leaderboardId, int limit) {
        log.warn("Nakama getLeaderboardRecords called before social integration is implemented. userId={}, leaderboardId={}", userId, leaderboardId);
        return Result.success(Collections.emptyList());
    }

    public Result<java.util.List<io.nakama.core.User>> getFriends(String userId) {
        log.warn("Nakama getFriends called before social integration is implemented. userId={}", userId);
        return Result.success(Collections.emptyList());
    }

    public Result<Void> addFriend(String userId, String friendId) {
        log.warn("Nakama addFriend called before social integration is implemented. userId={}, friendId={}", userId, friendId);
        return Result.error("社交能力暂未接入");
    }
}
