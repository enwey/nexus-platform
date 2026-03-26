package com.nexus.platform.controller;

import com.nexus.platform.dto.NakamaTypes.FriendInfo;
import com.nexus.platform.dto.NakamaTypes.LeaderboardRecordInfo;
import com.nexus.platform.dto.NakamaTypes.SessionInfo;
import com.nexus.platform.dto.Result;
import com.nexus.platform.service.NakamaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
public class NakamaController {
    private final NakamaService nakamaService;

    @PostMapping("/authenticate")
    public Result<SessionInfo> authenticate(@RequestBody AuthenticateRequest request) {
        return nakamaService.authenticate(request.userId(), request.token());
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody SocialRegisterRequest request) {
        return nakamaService.register(request.username(), request.password());
    }

    @PostMapping("/storage/write")
    public Result<Void> writeStorage(@RequestBody StorageWriteRequest request) {
        return nakamaService.writeStorage(request.userId(), request.key(), request.value());
    }

    @GetMapping("/storage/read")
    public Result<String> readStorage(@RequestParam String userId, @RequestParam String key) {
        return nakamaService.readStorage(userId, key);
    }

    @PostMapping("/leaderboard/write")
    public Result<Void> writeLeaderboardRecord(@RequestBody LeaderboardRequest request) {
        return nakamaService.writeLeaderboardRecord(
                request.userId(),
                request.leaderboardId(),
                request.score(),
                request.subscore()
        );
    }

    @GetMapping("/leaderboard/list")
    public Result<java.util.List<LeaderboardRecordInfo>> getLeaderboardRecords(
            @RequestParam String userId,
            @RequestParam String leaderboardId,
            @RequestParam(defaultValue = "10") int limit) {
        return nakamaService.getLeaderboardRecords(userId, leaderboardId, limit);
    }

    @GetMapping("/friends/list")
    public Result<java.util.List<FriendInfo>> getFriends(@RequestParam String userId) {
        return nakamaService.getFriends(userId);
    }

    @PostMapping("/friends/add")
    public Result<Void> addFriend(@RequestParam String userId, @RequestParam String friendId) {
        return nakamaService.addFriend(userId, friendId);
    }
}

record AuthenticateRequest(String userId, String token) {}
record SocialRegisterRequest(String username, String password) {}
record StorageWriteRequest(String userId, String key, String value) {}
record LeaderboardRequest(String userId, String leaderboardId, long score, long subscore) {}
