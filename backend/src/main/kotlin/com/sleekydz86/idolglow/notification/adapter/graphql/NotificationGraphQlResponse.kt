package com.sleekydz86.idolglow.notification.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlValue
import com.sleekydz86.idolglow.notification.application.dto.NotificationResponse
import com.sleekydz86.idolglow.notification.domain.Notification

data class NotificationGraphQlResponse(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val link: String?,
    val readAt: String?,
    val createdAt: String?,
) {
    companion object {
        fun from(response: NotificationResponse): NotificationGraphQlResponse =
            NotificationGraphQlResponse(
                id = response.id.asGraphQlId(),
                type = response.type.name,
                title = response.title,
                message = response.message,
                link = response.link,
                readAt = response.readAt.asGraphQlValue(),
                createdAt = response.createdAt.asGraphQlValue()
            )

        fun from(notification: Notification): NotificationGraphQlResponse =
            NotificationGraphQlResponse(
                id = notification.id.asGraphQlId(),
                type = notification.type.name,
                title = notification.title,
                message = notification.message,
                link = notification.link,
                readAt = notification.readAt.asGraphQlValue(),
                createdAt = notification.createdAt.asGraphQlValue()
            )
    }
}
