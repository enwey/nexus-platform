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
    private String termsTitle = "";
    private String termsHtml = "";
    private String privacyTitle = "";
    private String privacyHtml = "";
}
