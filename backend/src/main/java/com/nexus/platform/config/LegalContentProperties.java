package com.nexus.platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "platform.legal")
public class LegalContentProperties {
    private String termsTitle = "Nexus User Agreement";
    private String termsHtml = "<p>Welcome to Nexus Platform.</p><p>This is the default user agreement content.</p>";
    private String privacyTitle = "Nexus Privacy Policy";
    private String privacyHtml = "<p>Welcome to Nexus Platform.</p><p>This is the default privacy policy content.</p>";
}
