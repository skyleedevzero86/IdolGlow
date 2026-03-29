package com.sleekydz86.idolglow.notification.application

import com.sleekydz86.idolglow.notification.domain.Notification
import com.sleekydz86.idolglow.notification.domain.NotificationPreferenceRepository
import com.sleekydz86.idolglow.notification.domain.NotificationRepository
import com.sleekydz86.idolglow.notification.domain.NotificationType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional
@Service
class NotificationCommandService(
    private val notificationRepository: NotificationRepository,
    private val notificationPreferenceRepository: NotificationPreferenceRepository,
    private val notificationEventPublisher: NotificationEventPublisher,
) {

    fun create(
        userId: Long,
        type: NotificationType,
        title: String,
        message: String,
        link: String? = null,
    ): Notification? {
        if (notificationPreferenceRepository.isDisabled(userId, type)) return null
        val notification = notificationRepository.save(
            Notification(userId = userId, type = type, title = title, message = message, link = link)
        )
        notificationEventPublisher.publishCreated(notification.id, userId)
        return notification
    }

    fun markRead(notificationId: Long, userId: Long): Notification {
        val notification = notificationRepository.findById(notificationId)
            ?: throw IllegalArgumentException("알림을 찾을 수 없습니다: $notificationId")
        require(notification.userId == userId) { "본인 알림만 처리할 수 있습니다." }
        notification.markRead(LocalDateTime.now())
        return notification
    }

    fun markAllRead(userId: Long) {
        notificationRepository.markAllReadByUserId(userId, LocalDateTime.now())
    }
}
