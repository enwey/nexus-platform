package com.nexus.platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "platform.game-package")
public class GamePackageProperties {
    private String masterKey = "nexus-platform-dev-master-key";
    private long runtimeTicketTtlSeconds = 120L;
    private String runtimeTicketSigningKey = "nexus-platform-dev-runtime-ticket-signing-key";
}
