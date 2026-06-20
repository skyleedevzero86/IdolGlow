package com.sleekydz86.idolglow.glowalert.application.dto

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
