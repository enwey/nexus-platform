package com.nexus.platform.config;

import com.nexus.platform.entity.User;
import com.nexus.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements CommandLineRunner {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${platform.bootstrap-admin.enabled}")
    private boolean enabled;

    @Value("${platform.bootstrap-admin.username}")
    private String username;

    @Value("${platform.bootstrap-admin.password}")
    private String password;

    @Value("${platform.bootstrap-admin.email}")
    private String email;

    @Override
    public void run(String... args) {
        if (!enabled || userRepository.existsByUsername(username)) {
            return;
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setEmail(email);
        admin.setRole(User.UserRole.ADMIN);
        userRepository.save(admin);

        log.info("Bootstrap admin created: username={}", username);
    }
}
