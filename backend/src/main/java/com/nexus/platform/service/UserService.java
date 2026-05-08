package com.nexus.platform.service;

import com.nexus.platform.dto.AuthResponse;
import com.nexus.platform.dto.Result;
import com.nexus.platform.dto.UserProfileDto;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;
    private final LoginSecurityService loginSecurityService;
    private final AccountOpsService accountOpsService;
    private final PasswordEncoder passwordEncoder;

    public Result<AuthResponse> register(String email, String password, String code, String accountType) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return Result.error("Email is required");
        }
        if (!isStrongPassword(password)) {
            return Result.error("Password must be at least 8 chars and include letters and digits");
        }
        User.UserRole role = resolveRegisterRole(accountType);
        if (role == null) {
            return Result.error("Invalid account type");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return Result.error("Email already registered");
        }
        Result<Void> verifyResult = accountOpsService.verifyRegisterCode(normalizedEmail, code);
        if (verifyResult.getCode() != 0) {
            return Result.error(verifyResult.getCode(), verifyResult.getMessage());
        }

        User user = new User();
        user.setUsername(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(normalizedEmail);
        user.setPhone(null);
        user.setRole(role);

        User savedUser = userRepository.save(user);
        String accessToken = authTokenService.issueAccessToken(savedUser);
        String refreshToken = authTokenService.issueRefreshToken(savedUser);
        return Result.success(new AuthResponse(accessToken, refreshToken, UserProfileDto.from(savedUser)));
    }

    public Result<AuthResponse> login(String loginId, String password, String clientIp, String deviceId, String userAgent) {
        String normalizedLoginId = normalizeEmail(loginId);
        if (normalizedLoginId == null) {
            return Result.error("Email is required");
        }
        String blockedReason = loginSecurityService.getBlockReason(normalizedLoginId, clientIp, deviceId);
        if (blockedReason != null) {
            return Result.error(429, blockedReason);
        }

        User user = userRepository.findByEmailIgnoreCase(normalizedLoginId).orElse(null);
        if (user == null) {
            user = userRepository.findByUsernameIgnoreCase(normalizedLoginId).orElse(null);
        }
        if (user == null) {
            loginSecurityService.onLoginFailed(normalizedLoginId, clientIp, deviceId, userAgent, "User not found");
            return Result.error("User not found");
        }
        String governanceBlockReason = accountOpsService.getAccountBlockReason(user);
        if (governanceBlockReason != null) {
            loginSecurityService.onLoginFailed(normalizedLoginId, clientIp, deviceId, userAgent, governanceBlockReason);
            return Result.error(403, governanceBlockReason);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            loginSecurityService.onLoginFailed(normalizedLoginId, clientIp, deviceId, userAgent, "Incorrect password");
            return Result.error("Incorrect password");
        }

        loginSecurityService.onLoginSuccess(normalizedLoginId, user.getId(), clientIp, deviceId, userAgent);
        String accessToken = authTokenService.issueAccessToken(user);
        String refreshToken = authTokenService.issueRefreshToken(user);
        accountOpsService.recordDeviceLogin(user, clientIp, authTokenService.extractDeviceId(accessToken));
        return Result.success(new AuthResponse(accessToken, refreshToken, UserProfileDto.from(user)));
    }

    public Result<AuthResponse> refresh(String refreshToken) {
        AuthTokenService.TokenPair pair = authTokenService.rotateByRefreshToken(refreshToken);
        if (pair == null) {
            return Result.error("Refresh token is invalid or expired");
        }
        String governanceBlockReason = accountOpsService.getAccountBlockReason(pair.user());
        if (governanceBlockReason != null) {
            authTokenService.markUserTokensInvalidBefore(pair.user().getId());
            return Result.error(403, governanceBlockReason);
        }
        return Result.success(new AuthResponse(pair.accessToken(), pair.refreshToken(), UserProfileDto.from(pair.user())));
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            authTokenService.invalidate(token);
        }
    }

    public UserProfileDto findById(Long id) {
        User user = userRepository.findById(id).orElse(null);
        return user == null ? null : UserProfileDto.from(user);
    }

    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            }
            if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String value = email.trim().toLowerCase();
        return value.isEmpty() ? null : value;
    }

    private User.UserRole resolveRegisterRole(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            return User.UserRole.PLAYER;
        }
        String normalized = accountType.trim().toUpperCase();
        return switch (normalized) {
            case "PLAYER", "CONSUMER" -> User.UserRole.PLAYER;
            case "DEVELOPER" -> User.UserRole.DEVELOPER;
            default -> null;
        };
    }
}
