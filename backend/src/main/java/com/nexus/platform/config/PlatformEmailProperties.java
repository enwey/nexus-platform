package com.nexus.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "platform.email")
public class PlatformEmailProperties {
    private boolean enabled = false;
    private String fromAddress = "noreply@nexus.local";
    private String fromName = "Nexus Platform";
}
