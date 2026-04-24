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

    @Value("${platform.game-package.master-key:nexus-platform-dev-master-key}")
    private String gamePackageMasterKey;

    @Value("${platform.game-package.runtime-ticket-signing-key:nexus-platform-dev-runtime-ticket-signing-key}")
    private String runtimeTicketSigningKey;

    @Value("${platform.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${platform.email.from-address:}")
    private String emailFromAddress;

    @Value("${spring.mail.host:}")
    private String mailHost;

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

        if (gamePackageMasterKey == null || gamePackageMasterKey.isBlank()
                || "nexus-platform-dev-master-key".equals(gamePackageMasterKey)
                || gamePackageMasterKey.length() < 32) {
            throw new IllegalStateException(
                    "Insecure game package master key detected. Please set PLATFORM_GAME_PACKAGE_MASTER_KEY with a strong value.");
        }

        if (runtimeTicketSigningKey == null || runtimeTicketSigningKey.isBlank()
                || "nexus-platform-dev-runtime-ticket-signing-key".equals(runtimeTicketSigningKey)
                || runtimeTicketSigningKey.length() < 32) {
            throw new IllegalStateException(
                    "Insecure runtime ticket signing key detected. Please set PLATFORM_GAME_PACKAGE_RUNTIME_TICKET_SIGNING_KEY with a strong value.");
        }

        if (!emailEnabled) {
            throw new IllegalStateException("Email delivery must be enabled. Please set PLATFORM_EMAIL_ENABLED=true.");
        }

        if (mailHost == null || mailHost.isBlank()) {
            throw new IllegalStateException("Email delivery is enabled but SPRING_MAIL_HOST is not set.");
        }

        if (emailFromAddress == null || emailFromAddress.isBlank()) {
            throw new IllegalStateException("Email delivery is enabled but PLATFORM_EMAIL_FROM is not set.");
        }
    }
}
