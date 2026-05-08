package com.nexus.platform.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

class SecuritySanityCheckTest {

    @Test
    void shouldRejectProductionWhenCorsOriginsAreMissing() {
        PlatformCorsProperties corsProperties = new PlatformCorsProperties();
        SecuritySanityCheck check = new SecuritySanityCheck(
                new MockEnvironment().withProperty("spring.profiles.active", "prod"),
                corsProperties
        );
        applyBaseSafeValues(check);
        ReflectionTestUtils.setField(check, "publicBaseUrl", "https://api.nexus.example.com/api/v1");

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> check.run());
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("CORS origin patterns"));
    }

    @Test
    void shouldPassWhenProductionSecurityAndLegalConfigAreProvided() {
        PlatformCorsProperties corsProperties = new PlatformCorsProperties();
        corsProperties.setAllowedOriginPatterns(List.of("https://ops.nexus.example.com", "https://dev.nexus.example.com"));
        SecuritySanityCheck check = new SecuritySanityCheck(
                new MockEnvironment().withProperty("spring.profiles.active", "prod"),
                corsProperties
        );
        applyBaseSafeValues(check);
        ReflectionTestUtils.setField(check, "publicBaseUrl", "https://api.nexus.example.com/api/v1");
        ReflectionTestUtils.setField(check, "termsTitle", "Terms of Service");
        ReflectionTestUtils.setField(check, "termsHtml", "<p>terms</p>");
        ReflectionTestUtils.setField(check, "privacyTitle", "Privacy Policy");
        ReflectionTestUtils.setField(check, "privacyHtml", "<p>privacy</p>");
        ReflectionTestUtils.setField(check, "appSchemeTemplate", "nexus://game/%s");
        ReflectionTestUtils.setField(check, "androidInstallUrl", "https://play.google.com/store/apps/details?id=com.nexus.platform");
        ReflectionTestUtils.setField(check, "iosInstallUrl", "https://apps.apple.com/app/id123");
        ReflectionTestUtils.setField(check, "otherInstallUrl", "https://download.nexus.example.com");

        assertDoesNotThrow(() -> check.run());
    }

    private void applyBaseSafeValues(SecuritySanityCheck check) {
        ReflectionTestUtils.setField(check, "allowInsecureDefaults", false);
        ReflectionTestUtils.setField(check, "bootstrapAdminEnabled", false);
        ReflectionTestUtils.setField(check, "adminPassword", "");
        ReflectionTestUtils.setField(check, "jwtSecret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(check, "gamePackageMasterKey", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(check, "runtimeTicketSigningKey", "abcdefghijklmnopqrstuvwxyz123456");
        ReflectionTestUtils.setField(check, "emailEnabled", true);
        ReflectionTestUtils.setField(check, "mailHost", "smtp.example.com");
        ReflectionTestUtils.setField(check, "emailFromAddress", "noreply@example.com");
        ReflectionTestUtils.setField(check, "termsTitle", "");
        ReflectionTestUtils.setField(check, "termsHtml", "<p>default user agreement content</p>");
        ReflectionTestUtils.setField(check, "privacyTitle", "");
        ReflectionTestUtils.setField(check, "privacyHtml", "<p>default privacy policy content</p>");
        ReflectionTestUtils.setField(check, "appSchemeTemplate", "");
        ReflectionTestUtils.setField(check, "androidInstallUrl", "");
        ReflectionTestUtils.setField(check, "iosInstallUrl", "");
        ReflectionTestUtils.setField(check, "otherInstallUrl", "");
    }
}
