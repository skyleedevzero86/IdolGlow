package com.sleekydz86.idolglow.notification.application

import com.sleekydz86.idolglow.notification.application.event.NotificationCreatedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class NotificationEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun publishCreated(notificationId: Long, userId: Long) {
        applicationEventPublisher.publishEvent(
            NotificationCreatedEvent(
                notificationId = notificationId,
                userId = userId
            )
        )
    }
}
