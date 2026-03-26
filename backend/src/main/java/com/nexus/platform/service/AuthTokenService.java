package com.nexus.platform.service;

import com.nexus.platform.entity.User;
import com.nexus.platform.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthTokenService {
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.ttl-hours:2}")
    private long accessTtlHours;

    @Value("${security.jwt.refresh-ttl-days:14}")
    private long refreshTtlDays;

    public String issueToken(User user) {
        return issueAccessToken(user);
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        Instant expireAt = now.plus(Duration.ofHours(accessTtlHours));
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name())
                .claim("typ", "access")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(getSigningKey())
                .compact();
    }

    public String issueRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expireAt = now.plus(Duration.ofDays(refreshTtlDays));
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("typ", "refresh")
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(getSigningKey())
                .compact();

        stringRedisTemplate.opsForValue().set(refreshKey(user.getId(), jti), "1", Duration.between(now, expireAt));
        return token;
    }

    public TokenPair rotateByRefreshToken(String refreshToken) {
        try {
            Claims claims = parseClaims(refreshToken);
            if (!"refresh".equals(claims.get("typ"))) {
                return null;
            }

            Long userId = Long.valueOf(claims.getSubject());
            String refreshJti = claims.getId();
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(refreshKey(userId, refreshJti)))) {
                return null;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return null;
            }

            stringRedisTemplate.delete(refreshKey(userId, refreshJti));
            String newAccessToken = issueAccessToken(user);
            String newRefreshToken = issueRefreshToken(user);
            return new TokenPair(newAccessToken, newRefreshToken, user);
        } catch (Exception ignore) {
            return null;
        }
    }

    public User resolveUser(String token) {
        try {
            Claims claims = parseClaims(token);
            if (!"access".equals(claims.get("typ"))) {
                return null;
            }
            String jti = claims.getId();
            if (jti != null && Boolean.TRUE.equals(stringRedisTemplate.hasKey(denylistKey(jti)))) {
                return null;
            }
            String userId = claims.getSubject();
            return userRepository.findById(Long.valueOf(userId)).orElse(null);
        } catch (Exception ignore) {
            return null;
        }
    }

    public void invalidate(String token) {
        try {
            Claims claims = parseClaims(token);
            String type = String.valueOf(claims.get("typ"));
            String jti = claims.getId();
            Date expiration = claims.getExpiration();
            if (jti == null || expiration == null) {
                return;
            }

            Instant now = Instant.now();
            Instant exp = expiration.toInstant();
            if (exp.isAfter(now)) {
                Duration ttl = Duration.between(now, exp);
                if ("access".equals(type)) {
                    stringRedisTemplate.opsForValue().set(denylistKey(jti), "1", ttl);
                } else if ("refresh".equals(type)) {
                    Long userId = Long.valueOf(claims.getSubject());
                    stringRedisTemplate.delete(refreshKey(userId, jti));
                }
            }
        } catch (Exception ignore) {
            // Ignore malformed tokens.
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String denylistKey(String jti) {
        return "auth:denylist:" + jti;
    }

    private String refreshKey(Long userId, String jti) {
        return "auth:refresh:" + userId + ":" + jti;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("SECURITY_JWT_SECRET must be at least 32 characters.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public record TokenPair(String accessToken, String refreshToken, User user) {
    }
}

