package com.nexus.platform.service;

import com.nexus.platform.entity.User;
import com.nexus.platform.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthTokenService {
    private static final Duration TOKEN_TTL = Duration.ofHours(12);

    private final UserRepository userRepository;
    private final Map<String, SessionRecord> sessions = new ConcurrentHashMap<>();

    public String issueToken(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new SessionRecord(user.getId(), Instant.now().plus(TOKEN_TTL)));
        return token;
    }

    public User resolveUser(String token) {
        SessionRecord session = sessions.get(token);
        if (session == null) {
            return null;
        }

        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return null;
        }

        return userRepository.findById(session.userId()).orElse(null);
    }

    public void invalidate(String token) {
        if (token != null && !token.isBlank()) {
            sessions.remove(token);
        }
    }

    private record SessionRecord(Long userId, Instant expiresAt) {
    }
}
