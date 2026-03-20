package com.nexus.platform.service;

import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Result<User> register(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            return Result.error("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);

        User savedUser = userRepository.save(user);
        return Result.success(savedUser);
    }

    public Result<User> login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null) {
            return Result.error("用户不存在");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.error("密码错误");
        }

        return Result.success(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
