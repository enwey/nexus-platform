package com.nexus.platform.domain.model

import java.time.LocalDateTime

data class DeviceSession(
    val deviceId: String,
    val deviceName: String,
    val model: String,
    val ip: String,
    val lastActiveAt: String,
    val current: Boolean
)

data class BillingRecord(
    val id: Long,
    val type: String,
    val title: String,
    val subtitle: String,
    val amount: String,
    val createdAt: String
)

data class BillingDetail(
    val id: Long,
    val type: String,
    val title: String,
    val subtitle: String,
    val amount: String,
    val createdAt: String,
    val receiptUrl: String
)

data class ReferralSummary(
    val inviteCount: Long,
    val totalReward: String,
    val referralLink: String
)

data class ReferralRecord(
    val id: Long,
    val title: String,
    val subtitle: String,
    val reward: String,
    val createdAt: String
)
