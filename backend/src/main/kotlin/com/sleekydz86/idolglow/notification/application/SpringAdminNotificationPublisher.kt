package com.sleekydz86.idolglow.notification.application

import com.sleekydz86.idolglow.notification.application.event.AdminNotificationRequestedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SpringAdminNotificationPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : AdminNotificationPublisher {
    override fun publish(event: AdminNotificationRequestedEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
