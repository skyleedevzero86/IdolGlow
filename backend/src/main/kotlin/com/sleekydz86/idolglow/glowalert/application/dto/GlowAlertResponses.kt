package com.sleekydz86.idolglow.glowalert.application.dto

data class GlowAlertPageResponse(
    val items: List<GlowAlertItemResponse>,
    val categories: List<GlowAlertCategoryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val tab: String,
    val activeCategory: String,
)

data class GlowAlertItemResponse(
    val id: Long,
    val tab: String,
    val category: String,
    val categoryLabel: String,
    val senderName: String,
    val channelLabel: String?,
    val message: String,
    val receivedAt: String,
    val receivedAtLabel: String,
    val iconText: String,
    val iconTone: String,
    val unread: Boolean,
)

data class GlowAlertCategoryResponse(
    val id: String,
    val label: String,
    val count: Long,
)

data class GlowAlertUnreadCountResponse(
    val count: Long,
)
