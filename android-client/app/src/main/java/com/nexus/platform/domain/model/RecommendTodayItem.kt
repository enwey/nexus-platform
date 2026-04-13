package com.nexus.platform.domain.model

import java.io.Serializable

data class RecommendTodayItem(
    val appId: String,
    val gameName: String,
    val gameCategory: String,
    val gameIconUrl: String,
    val cardCategory: String,
    val cardTitle: String,
    val coverUrl: String,
    val articleTag: String,
    val articleTitle: String,
    val articleBody: String,
    val actionText: String
) : Serializable
