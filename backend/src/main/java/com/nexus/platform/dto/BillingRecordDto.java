package com.nexus.platform.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillingRecordDto(
        Long id,
        String type,
        String title,
        String subtitle,
        BigDecimal amount,
        LocalDateTime createdAt
) {
}
