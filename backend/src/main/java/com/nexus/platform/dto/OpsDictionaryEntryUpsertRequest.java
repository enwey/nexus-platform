package com.nexus.platform.dto;

public record OpsDictionaryEntryUpsertRequest(
        String dictType,
        String dictKey,
        String dictLabel,
        String dictValue,
        Integer sortOrder,
        String status
) {
}
