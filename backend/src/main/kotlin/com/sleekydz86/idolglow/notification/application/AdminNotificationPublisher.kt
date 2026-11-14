package com.sleekydz86.idolglow.notification.application

import com.sleekydz86.idolglow.notification.application.event.AdminNotificationRequestedEvent

fun interface AdminNotificationPublisher {
    fun publish(event: AdminNotificationRequestedEvent)

    companion object {
        val NONE = AdminNotificationPublisher { }
    }
}
