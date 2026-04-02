package com.nexus.platform.domain.model

data class WalletSummary(
    val balance: String,
    val frozenBalance: String,
    val availableBalance: String,
    val todayIncome: String,
    val totalIncome: String
)
