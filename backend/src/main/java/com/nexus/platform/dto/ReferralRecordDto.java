package com.nexus.platform.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReferralRecordDto(
        Long id,
        String title,
        String subtitle,
        BigDecimal reward,
        LocalDateTime createdAt
) {
}
