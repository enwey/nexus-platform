package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsDictionaryEntryDto(
        Long id,
        String dictType,
        String dictKey,
        String dictLabel,
        String dictValue,
        Integer sortOrder,
        String status,
        String updatedBy,
        LocalDateTime updatedAt
) {
}
