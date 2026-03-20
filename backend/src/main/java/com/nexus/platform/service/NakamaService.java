package com.nexus.platform.service;

import com.nexus.platform.dto.Result;
import io.nakama.apiclient.ApiClient;
import io.nakama.apiclient.SessionClient;
import io.nakama.core.DefaultSession;
import io.nakama.core.UserAccount;
import io.nakama.core.account.AccountDevice;
import io.nakama.core.account.AccountFacebook;
import io.nakama.core.account.AccountGameCenter;
import io.nakama.core.account.AccountGoogle;
import io.nakama.core.account.AccountSteam;
import io.nakama.core.account.AccountCustom;
import io.nakama.core.session.AuthenticateRequest;
import io.nakama.core.session.AuthenticateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class NakamaService {
    private final ApiClient nakamaApiClient;
    private final SessionClient nakamaSessionClient;

    public Result<DefaultSession> authenticate(String userId, String token) {
        try {
            UserAccount account = UserAccount.builder()
                    .id(userId)
                    .build();

            AuthenticateRequest request = AuthenticateRequest.builder()
                    .account(account)
                    .token(token)
                    .build();

            DefaultSession session = nakamaApiClient.authenticate(request).get();
            
            return Result.success(session);
        } catch (Exception e) {
            log.error("Nakama authentication failed", e);
            return Result.error("认证失败: " + e.getMessage());
        }
    }

    public Result<String> register(String username, String password) {
        try {
            UserAccount account = UserAccount.builder()
                    .id(username)
                    .build();

            AuthenticateRequest request = AuthenticateRequest.builder()
                    .account(account)
                    .create(true)
                    .username(username)
                    .password(password)
                    .build();

            DefaultSession session = nakamaApiClient.authenticate(request).get();
            
            return Result.success(session.getToken());
        } catch (Exception e) {
            log.error("Nakama registration failed", e);
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    public Result<Void> writeStorage(String userId, String key, String value) {
        try {
            DefaultSession session = nakamaSessionClient.getSession(userId);
            
            if (session == null) {
                return Result.error("用户未登录");
            }

            nakamaApiClient.writeStorageObjects(session, key, value).get();
            
            return Result.success();
        } catch (Exception e) {
            log.error("Nakama write storage failed", e);
            return Result.error("写入云存档失败: " + e.getMessage());
        }
    }

    public Result<String> readStorage(String userId, String key) {
        try {
            DefaultSession session = nakamaSessionClient.getSession(userId);
            
            if (session == null) {
                return Result.error("用户未登录");
            }

            var storageObjects = nakamaApiClient.readStorageObjects(session, key).get();
            
            if (storageObjects.isEmpty()) {
                return Result.error("云存档不存在");
            }
            
            return Result.success(storageObjects.get(0).getValue());
        } catch (Exception e) {
            log.error("Nakama read storage failed", e);
            return Result.error("读取云存档失败: " + e.getMessage());
        }
    }

    public Result<Void> writeLeaderboardRecord(String userId, String leaderboardId, long score, long subscore) {
        try {
            DefaultSession session = nakamaSessionClient.getSession(userId);
            
            if (session == null) {
                return Result.error("用户未登录");
            }

            nakamaApiClient.writeLeaderboardRecord(session, leaderboardId, userId, score, subscore).get();
            
            return Result.success();
        } catch (Exception e) {
            log.error("Nakama write leaderboard failed", e);
            return Result.error("写入排行榜失败: " + e.getMessage());
        }
    }

    public Result<java.util.List<io.nakama.api.LeaderboardRecord>> getLeaderboardRecords(String userId, String leaderboardId, int limit) {
        try {
            DefaultSession session = nakamaSessionClient.getSession(userId);
            
            if (session == null) {
                return Result.error("用户未登录");
            }

            var records = nakamaApiClient.listLeaderboardRecords(session, leaderboardId, null, limit, null).get();
            
            return Result.success(records);
        } catch (Exception e) {
            log.error("Nakama get leaderboard failed", e);
            return Result.error("获取排行榜失败: " + e.getMessage());
        }
    }

    public Result<java.util.List<io.nakama.core.User>> getFriends(String userId) {
        try {
            DefaultSession session = nakamaSessionClient.getSession(userId);
            
            if (session == null) {
                return Result.error("用户未登录");
            }

            var users = nakamaApiClient.listFriends(session, 0, 100, null).get();
            
            return Result.success(users);
        } catch (Exception e) {
            log.error("Nakama get friends failed", e);
            return Result.error("获取好友列表失败: " + e.getMessage());
        }
    }

    public Result<Void> addFriend(String userId, String friendId) {
        try {
            DefaultSession session = nakamaSessionClient.getSession(userId);
            
            if (session == null) {
                return Result.error("用户未登录");
            }

            nakamaApiClient.addFriends(session, friendId).get();
            
            return Result.success();
        } catch (Exception e) {
            log.error("Nakama add friend failed", e);
            return Result.error("添加好友失败: " + e.getMessage());
        }
    }
}
