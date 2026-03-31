package com.nexus.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameUpdateCheckResponse {
    private boolean hasUpdate;
    private boolean forceUpdate;
    private boolean updateReady;
    private String latestVersion;
    private String downloadUrl;
    private String md5;
}

