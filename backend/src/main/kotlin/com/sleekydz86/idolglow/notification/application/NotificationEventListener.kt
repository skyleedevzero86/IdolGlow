package com.sleekydz86.idolglow.notification.application

import com.sleekydz86.idolglow.notification.application.dto.NotificationResponse
import com.sleekydz86.idolglow.notification.application.event.NotificationCreatedEvent
import com.sleekydz86.idolglow.notification.domain.NotificationRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class NotificationEventListener(
    private val notificationRepository: NotificationRepository,
    private val notificationStreamService: NotificationStreamService,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleNotificationCreated(event: NotificationCreatedEvent) {
        val notification = notificationRepository.findById(event.notificationId) ?: return
        notificationStreamService.sendToUser(
            event.userId,
            NotificationResponse.Companion.from(notification)
        )
    }
}