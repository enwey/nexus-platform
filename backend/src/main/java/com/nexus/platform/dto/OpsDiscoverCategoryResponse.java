package com.nexus.platform.dto;

public record OpsDiscoverCategoryResponse(
        Long id,
        String name,
        Integer sortOrder,
        Long usageCount,
        Long bannerUsageCount,
        Long recommendationUsageCount
) {
}
