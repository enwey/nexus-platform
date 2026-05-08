package com.nexus.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

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

    @Value("${platform.public-base-url:}")
    private String publicBaseUrl;

    @Value("${platform.legal.terms-html:}")
    private String termsHtml;

    @Value("${platform.legal.privacy-html:}")
    private String privacyHtml;

    @Value("${platform.legal.terms-title:}")
    private String termsTitle;

    @Value("${platform.legal.privacy-title:}")
    private String privacyTitle;

    @Value("${platform.share.android-install-url:}")
    private String androidInstallUrl;

    @Value("${platform.share.ios-install-url:}")
    private String iosInstallUrl;

    @Value("${platform.share.other-install-url:}")
    private String otherInstallUrl;

    @Value("${platform.share.app-scheme-template:}")
    private String appSchemeTemplate;

    private final PlatformCorsProperties corsProperties;

    private final Environment environment;

    public SecuritySanityCheck(Environment environment, PlatformCorsProperties corsProperties) {
        this.environment = environment;
        this.corsProperties = corsProperties;
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

        validateCors(enforce, prodProfile);
        validatePublicFacingConfiguration(enforce, prodProfile);

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

    private void validateCors(boolean enforce, boolean prodProfile) {
        if (!enforce) {
            return;
        }
        List<String> allowedOrigins = corsProperties.getAllowedOriginPatterns();
        if (allowedOrigins == null || allowedOrigins.stream().noneMatch(origin -> origin != null && !origin.isBlank())) {
            throw new IllegalStateException("CORS origin patterns must be configured. Please set PLATFORM_CORS_ALLOWED_ORIGIN_PATTERNS.");
        }
        if (prodProfile && allowedOrigins.stream().anyMatch(this::isLocalAddress)) {
            throw new IllegalStateException("CORS origin patterns must not contain localhost or loopback origins in production.");
        }
    }

    private void validatePublicFacingConfiguration(boolean enforce, boolean prodProfile) {
        if (!enforce) {
            return;
        }
        validateAbsoluteUrl(publicBaseUrl, "PLATFORM_PUBLIC_BASE_URL", prodProfile);
        requireText(termsTitle, "PLATFORM_LEGAL_TERMS_TITLE");
        requireText(termsHtml, "PLATFORM_LEGAL_TERMS_HTML");
        requireText(privacyTitle, "PLATFORM_LEGAL_PRIVACY_TITLE");
        requireText(privacyHtml, "PLATFORM_LEGAL_PRIVACY_HTML");
        requireText(appSchemeTemplate, "PLATFORM_SHARE_APP_SCHEME_TEMPLATE");
        if (!appSchemeTemplate.contains("%s")) {
            throw new IllegalStateException("PLATFORM_SHARE_APP_SCHEME_TEMPLATE must contain a %s placeholder for the app id.");
        }
        validateAbsoluteUrl(androidInstallUrl, "PLATFORM_SHARE_ANDROID_INSTALL_URL", prodProfile);
        validateAbsoluteUrl(iosInstallUrl, "PLATFORM_SHARE_IOS_INSTALL_URL", prodProfile);
        validateAbsoluteUrl(otherInstallUrl, "PLATFORM_SHARE_OTHER_INSTALL_URL", prodProfile);
    }

    private void requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must be configured with non-empty production content.");
        }
    }

    private void validateAbsoluteUrl(String value, String propertyName, boolean prodProfile) {
        requireText(value, propertyName);
        try {
            URI uri = new URI(value);
            if (!uri.isAbsolute() || uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalStateException(propertyName + " must be a valid absolute URL.");
            }
            if (prodProfile && isLocalAddress(uri.getHost())) {
                throw new IllegalStateException(propertyName + " must not point to localhost or loopback hosts in production.");
            }
        } catch (URISyntaxException e) {
            throw new IllegalStateException(propertyName + " must be a valid absolute URL.", e);
        }
    }

    private boolean isLocalAddress(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.toLowerCase();
        return normalized.contains("localhost")
                || normalized.contains("127.0.0.1")
                || normalized.contains("::1");
    }
}
