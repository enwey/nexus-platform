package com.nexus.platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "platform.share")
public class ShareLandingProperties {
    private String appSchemeTemplate = "";
    private String androidInstallUrl = "";
    private String iosInstallUrl = "";
    private String otherInstallUrl = "";
}
