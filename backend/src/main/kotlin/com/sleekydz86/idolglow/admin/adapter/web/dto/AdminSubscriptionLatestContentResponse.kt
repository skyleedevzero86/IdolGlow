package com.sleekydz86.idolglow.admin.adapter.web.dto

data class AdminSubscriptionLatestContentResponse(
    val contentType: String,
    val contentTypeLabel: String,
    val title: String,
    val slug: String,
    val summary: String?,
    val publishedAt: String?,
)
