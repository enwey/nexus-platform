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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Result<AuthResponse> register(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            return Result.error("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);

        User savedUser = userRepository.save(user);
        String token = authTokenService.issueToken(savedUser);
        return Result.success(new AuthResponse(token, UserProfileDto.from(savedUser)));
    }

    public Result<AuthResponse> login(String username, String password) {
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return Result.error("用户不存在");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.error("密码错误");
        }

        String token = authTokenService.issueToken(user);
        return Result.success(new AuthResponse(token, UserProfileDto.from(user)));
    }

    public UserProfileDto findById(Long id) {
        User user = userRepository.findById(id).orElse(null);
        return user == null ? null : UserProfileDto.from(user);
    }
}
