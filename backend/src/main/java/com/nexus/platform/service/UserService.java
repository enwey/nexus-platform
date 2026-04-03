package com.nexus.platform.service;

import com.nexus.platform.dto.AuthResponse;
import com.nexus.platform.dto.Result;
import com.nexus.platform.dto.UserProfileDto;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;
    private final LoginSecurityService loginSecurityService;
    private final AccountOpsService accountOpsService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Result<AuthResponse> register(String username, String password, String email) {
        if (username == null || username.trim().length() < 3) {
            return Result.error("Username length must be at least 3");
        }
        if (!isStrongPassword(password)) {
            return Result.error("Password must be at least 8 chars and include letters and digits");
        }
        if (userRepository.existsByUsername(username.trim())) {
            return Result.error("Username already exists");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);

        User savedUser = userRepository.save(user);
        String accessToken = authTokenService.issueAccessToken(savedUser);
        String refreshToken = authTokenService.issueRefreshToken(savedUser);
        return Result.success(new AuthResponse(accessToken, refreshToken, UserProfileDto.from(savedUser)));
    }

    public Result<AuthResponse> login(String username, String password, String clientIp) {
        String blockedReason = loginSecurityService.getBlockReason(username, clientIp);
        if (blockedReason != null) {
            return Result.error(429, blockedReason);
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            loginSecurityService.onLoginFailed(username, clientIp);
            return Result.error("User not found");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            loginSecurityService.onLoginFailed(username, clientIp);
            return Result.error("Incorrect password");
        }

        loginSecurityService.onLoginSuccess(username, clientIp);
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
}
