package com.nexus.platform.dto;

import java.math.BigDecimal;

public record ReferralSummaryDto(
        long inviteCount,
        BigDecimal totalReward,
        String referralLink
) {
}
