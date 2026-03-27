package com.sleekydz86.idolglow.notification.application

import com.sleekydz86.idolglow.notification.application.dto.NotificationResponse
import com.sleekydz86.idolglow.notification.domain.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class NotificationQueryService(
    private val notificationRepository: NotificationRepository
) {

    fun findNotifications(userId: Long): List<NotificationResponse> =
        notificationRepository.findAllByUserId(userId)
            .map(NotificationResponse::from)
}
