package com.nexus.platform.controller;

import com.nexus.platform.dto.Result;
import com.nexus.platform.service.NakamaService;
import io.nakama.core.DefaultSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
public class NakamaController {
    private final NakamaService nakamaService;

    @PostMapping("/authenticate")
    public Result<DefaultSession> authenticate(@RequestBody AuthenticateRequest request) {
        return nakamaService.authenticate(request.getUserId(), request.getToken());
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterRequest request) {
        return nakamaService.register(request.getUsername(), request.getPassword());
    }

    @PostMapping("/storage/write")
    public Result<Void> writeStorage(@RequestBody StorageWriteRequest request) {
        return nakamaService.writeStorage(request.getUserId(), request.getKey(), request.getValue());
    }

    @GetMapping("/storage/read")
    public Result<String> readStorage(@RequestParam String userId, @RequestParam String key) {
        return nakamaService.readStorage(userId, key);
    }

    @PostMapping("/leaderboard/write")
    public Result<Void> writeLeaderboardRecord(@RequestBody LeaderboardRequest request) {
        return nakamaService.writeLeaderboardRecord(
            request.getUserId(),
            request.getLeaderboardId(),
            request.getScore(),
            request.getSubscore()
        );
    }

    @GetMapping("/leaderboard/list")
    public Result<java.util.List<io.nakama.api.LeaderboardRecord>> getLeaderboardRecords(
            @RequestParam String userId,
            @RequestParam String leaderboardId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return nakamaService.getLeaderboardRecords(userId, leaderboardId, limit);
    }

    @GetMapping("/friends/list")
    public Result<java.util.List<io.nakama.core.User>> getFriends(@RequestParam String userId) {
        return nakamaService.getFriends(userId);
    }

    @PostMapping("/friends/add")
    public Result<Void> addFriend(@RequestParam String userId, @RequestParam String friendId) {
        return nakamaService.addFriend(userId, friendId);
    }
}

record AuthenticateRequest(String userId, String token) {}
record RegisterRequest(String username, String password) {}
record StorageWriteRequest(String userId, String key, String value) {}
record LeaderboardRequest(String userId, String leaderboardId, long score, long subscore) {}
