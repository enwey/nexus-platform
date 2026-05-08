package com.nexus.platform.dto;

import java.time.LocalDateTime;

public record OpsGiftCodeEntryDto(
        Long id,
        String code,
        String status,
        Long redeemedByUserId,
        LocalDateTime redeemedAt,
        LocalDateTime createdAt
) {
}
