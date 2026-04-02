package com.nexus.platform.dto;

import java.math.BigDecimal;

public record WalletSummaryDto(
        BigDecimal balance,
        BigDecimal frozenBalance,
        BigDecimal availableBalance,
        BigDecimal todayIncome,
        BigDecimal totalIncome
) {
}
