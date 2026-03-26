package com.nexus.platform.dto;

public final class NakamaTypes {
    private NakamaTypes() {
    }

    public record SessionInfo(String userId, String token) {}

    public record LeaderboardRecordInfo(String userId, long score, long subscore) {}

    public record FriendInfo(String userId, String username) {}
}

