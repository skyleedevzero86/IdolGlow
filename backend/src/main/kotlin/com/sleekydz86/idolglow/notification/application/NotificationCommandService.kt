package com.sleekydz86.idolglow.notification.application

import com.sleekydz86.idolglow.notification.domain.Notification
import com.sleekydz86.idolglow.notification.domain.NotificationRepository
import com.sleekydz86.idolglow.notification.domain.NotificationType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional
@Service
class NotificationCommandService(
    private val notificationRepository: NotificationRepository,
    private val notificationEventPublisher: NotificationEventPublisher,
) {

    fun create(
        userId: Long,
        type: NotificationType,
        title: String,
        message: String,
        link: String? = null,
    ): Notification {
        val notification = notificationRepository.save(
            Notification(
                userId = userId,
                type = type,
                title = title,
                message = message,
                link = link
            )
        )
        notificationEventPublisher.publishCreated(notification.id, userId)
        return notification
    }

    fun markRead(notificationId: Long, userId: Long): Notification {
        val notification = notificationRepository.findById(notificationId)
            ?: throw IllegalArgumentException("Notification not found: $notificationId")
        require(notification.userId == userId) { "Notification can be handled only by its owner." }
        notification.markRead(LocalDateTime.now())
        return notification
    }
}
