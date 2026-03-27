package com.sleekydz86.idolglow.notification.application.dto

import com.sleekydz86.idolglow.notification.domain.Notification
import com.sleekydz86.idolglow.notification.domain.NotificationType
import java.time.LocalDateTime

data class NotificationResponse(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val message: String,
    val link: String?,
    val readAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse =
            NotificationResponse(
                id = notification.id,
                type = notification.type,
                title = notification.title,
                message = notification.message,
                link = notification.link,
                readAt = notification.readAt,
                createdAt = notification.createdAt
            )
    }
}
