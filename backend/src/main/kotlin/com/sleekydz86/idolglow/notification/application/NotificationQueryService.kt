package com.sleekydz86.idolglow.notification.application

import com.sleekydz86.idolglow.notification.application.dto.NotificationResponse
import com.sleekydz86.idolglow.notification.application.dto.UnreadCountResponse
import com.sleekydz86.idolglow.notification.domain.NotificationRepository
import com.sleekydz86.idolglow.notification.domain.NotificationType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class NotificationQueryService(
    private val notificationRepository: NotificationRepository
) {

    fun findNotifications(userId: Long, type: NotificationType? = null): List<NotificationResponse> {
        val notifications = if (type != null) {
            notificationRepository.findAllByUserIdAndType(userId, type)
        } else {
            notificationRepository.findAllByUserId(userId)
        }
        return notifications.map(NotificationResponse::from)
    }

    fun countUnread(userId: Long): UnreadCountResponse =
        UnreadCountResponse(count = notificationRepository.countUnreadByUserId(userId))
}
