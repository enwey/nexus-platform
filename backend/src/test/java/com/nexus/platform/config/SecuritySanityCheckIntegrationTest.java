package com.nexus.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecuritySanityCheckIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(
                    PlatformCorsProperties.class,
                    LegalContentProperties.class,
                    ShareLandingProperties.class,
                    PlatformEmailProperties.class,
                    SecuritySanityCheck.class
            );

    @Test
    void shouldAcceptSecureProductionBaseline() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "platform.security.allow-insecure-defaults=false",
                        "platform.bootstrap-admin.enabled=false",
                        "security.jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef",
                        "platform.game-package.master-key=abcdefghijklmnopqrstuvwxyz123456",
                        "platform.game-package.runtime-ticket-signing-key=abcdefghijklmnopqrstuvwxyz654321",
                        "platform.email.enabled=true",
                        "platform.email.from-address=security@example.com",
                        "spring.mail.host=smtp.example.com",
                        "platform.public-base-url=https://api.example.com/api/v1",
                        "platform.legal.terms-title=Terms of Service",
                        "platform.legal.terms-html=<p>Production legal content</p>",
                        "platform.legal.privacy-title=Privacy Policy",
                        "platform.legal.privacy-html=<p>Production privacy content</p>",
                        "platform.share.app-scheme-template=nexus://game/%s",
                        "platform.share.android-install-url=https://download.example.com/android",
                        "platform.share.ios-install-url=https://download.example.com/ios",
                        "platform.share.other-install-url=https://download.example.com/web",
                        "platform.cors.allowed-origin-patterns=https://ops.example.com,https://dev.example.com"
                )
                .run(context -> assertThatCode(() -> context.getBean(SecuritySanityCheck.class).run())
                        .doesNotThrowAnyException());
    }

    @Test
    void shouldRejectLocalhostCorsInProduction() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "platform.security.allow-insecure-defaults=false",
                        "platform.bootstrap-admin.enabled=false",
                        "security.jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef",
                        "platform.game-package.master-key=abcdefghijklmnopqrstuvwxyz123456",
                        "platform.game-package.runtime-ticket-signing-key=abcdefghijklmnopqrstuvwxyz654321",
                        "platform.email.enabled=true",
                        "platform.email.from-address=security@example.com",
                        "spring.mail.host=smtp.example.com",
                        "platform.public-base-url=https://api.example.com/api/v1",
                        "platform.legal.terms-title=Terms of Service",
                        "platform.legal.terms-html=<p>Production legal content</p>",
                        "platform.legal.privacy-title=Privacy Policy",
                        "platform.legal.privacy-html=<p>Production privacy content</p>",
                        "platform.share.app-scheme-template=nexus://game/%s",
                        "platform.share.android-install-url=https://download.example.com/android",
                        "platform.share.ios-install-url=https://download.example.com/ios",
                        "platform.share.other-install-url=https://download.example.com/web",
                        "platform.cors.allowed-origin-patterns=http://localhost:5173,https://ops.example.com"
                )
                .run(context -> assertThatThrownBy(() -> context.getBean(SecuritySanityCheck.class).run())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("CORS origin"));
    }
}
