package com.nexus.platform.dto;

import java.util.List;

public record GameMetadataUpdateRequest(
        String name,
        String description,
        String iconUrl,
        String category,
        List<String> tags,
        String version
) {
}
