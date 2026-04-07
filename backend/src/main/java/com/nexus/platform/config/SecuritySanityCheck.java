package com.nexus.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SecuritySanityCheck implements CommandLineRunner {

    @Value("${platform.security.allow-insecure-defaults:false}")
    private boolean allowInsecureDefaults;

    @Value("${platform.bootstrap-admin.enabled:false}")
    private boolean bootstrapAdminEnabled;

    @Value("${platform.bootstrap-admin.password:}")
    private String adminPassword;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private final Environment environment;

    public SecuritySanityCheck(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        boolean prodProfile = false;
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                prodProfile = true;
                break;
            }
        }

        boolean enforce = prodProfile || !allowInsecureDefaults;
        if (!enforce) {
            log.warn("Security sanity check in relaxed mode. Do not use insecure defaults in production.");
            return;
        }

        if (bootstrapAdminEnabled && (adminPassword == null || adminPassword.isBlank())) {
            throw new IllegalStateException("Bootstrap admin is enabled but PLATFORM_BOOTSTRAP_ADMIN_PASSWORD is not set.");
        }

        if (bootstrapAdminEnabled && "admin123456".equals(adminPassword)) {
            throw new IllegalStateException("Insecure admin password detected. Please set PLATFORM_BOOTSTRAP_ADMIN_PASSWORD.");
        }

        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32 || jwtSecret.contains("change-this-in-dev-only")) {
            throw new IllegalStateException("Insecure JWT secret detected. Please set SECURITY_JWT_SECRET with a strong value.");
        }
    }
}

