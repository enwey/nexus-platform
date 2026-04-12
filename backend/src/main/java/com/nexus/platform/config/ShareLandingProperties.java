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
    private String appSchemeTemplate = "bringbox://game/%s";
    private String androidInstallUrl = "https://play.google.com/store/apps/details?id=com.nexus.platform";
    private String iosInstallUrl = "https://apps.apple.com";
    private String otherInstallUrl = "https://bringbox.com/download";
}
