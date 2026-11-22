package com.sleekydz86.idolglow.notification.application.event

import com.sleekydz86.idolglow.notification.domain.NotificationType

data class AdminNotificationRequestedEvent(
    val type: NotificationType,
    val title: String,
    val message: String,
    val link: String,
    val deduplicateByLink: Boolean = false,
)
