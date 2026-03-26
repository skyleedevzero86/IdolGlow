package com.sleekydz86.idolglow.notification.application.event

data class NotificationCreatedEvent(
    val notificationId: Long,
    val userId: Long,
)
